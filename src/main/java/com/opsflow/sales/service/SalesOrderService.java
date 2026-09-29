package com.opsflow.sales.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.inventory.service.ProductService;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.sales.domain.SalesOrder;
import com.opsflow.sales.domain.SalesOrderItem;
import com.opsflow.sales.domain.SalesOrderStatus;
import com.opsflow.sales.dto.CreateSalesOrderRequest;
import com.opsflow.sales.dto.SalesOrderItemRequest;
import com.opsflow.sales.dto.SalesOrderResponse;
import com.opsflow.sales.dto.UpdateSalesOrderStatusRequest;
import com.opsflow.sales.repository.SalesOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrganizationRepository organizationRepository;

    public SalesOrderService(
        SalesOrderRepository salesOrderRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        ProductService productService,
        OrganizationRepository organizationRepository
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public SalesOrderResponse createSalesOrder(UserPrincipal principal, CreateSalesOrderRequest request) {
        UUID organizationId = principal.getOrganizationId();

        Customer customer = customerRepository.findByIdAndOrganizationIdAndActiveTrue(request.customerId(), organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        long count = salesOrderRepository.countByOrganizationId(organizationId);
        String soNumber = String.format("SO-%d-%04d", LocalDate.now().getYear(), count + 1);

        SalesOrder so = new SalesOrder(
            organization,
            customer,
            soNumber,
            SalesOrderStatus.DRAFT,
            BigDecimal.ZERO,
            request.notes(),
            request.expectedDeliveryDate(),
            principal.getId(),
            principal.getUsername()
        );

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SalesOrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findByIdAndOrganizationIdAndActiveTrue(itemReq.productId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.productId()));

            BigDecimal subtotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(subtotal);

            SalesOrderItem item = new SalesOrderItem(
                so,
                product,
                itemReq.quantity(),
                itemReq.unitPrice(),
                subtotal
            );
            so.addItem(item);
        }

        so.setTotalAmount(totalAmount);
        SalesOrder savedSo = salesOrderRepository.save(so);

        return SalesOrderResponse.fromEntity(savedSo);
    }

    @Transactional
    public SalesOrderResponse updateStatus(UserPrincipal principal, UUID soId, UpdateSalesOrderStatusRequest request) {
        UUID organizationId = principal.getOrganizationId();
        SalesOrder so = findSalesOrderForTenant(organizationId, soId);

        SalesOrderStatus currentStatus = so.getStatus();
        SalesOrderStatus newStatus = request.status();

        if (currentStatus == SalesOrderStatus.FULFILLED || currentStatus == SalesOrderStatus.CANCELLED) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Cannot change status of a %s sales order", currentStatus)
            );
        }

        validateStateTransition(currentStatus, newStatus);

        if (newStatus == SalesOrderStatus.FULFILLED) {
            for (SalesOrderItem item : so.getItems()) {
                AdjustStockRequest adjustStockRequest = new AdjustStockRequest(
                    -item.getQuantity(),
                    MovementType.OUTBOUND,
                    String.format("SO %s fulfilled for customer %s", so.getSoNumber(), so.getCustomer().getName())
                );
                productService.adjustStock(principal, item.getProduct().getId(), adjustStockRequest);
            }
        }

        so.setStatus(newStatus);
        SalesOrder savedSo = salesOrderRepository.save(so);

        return SalesOrderResponse.fromEntity(savedSo);
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getSalesOrder(UserPrincipal principal, UUID soId) {
        SalesOrder so = findSalesOrderForTenant(principal.getOrganizationId(), soId);
        return SalesOrderResponse.fromEntity(so);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> listSalesOrders(UserPrincipal principal, Pageable pageable) {
        return salesOrderRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId(), pageable)
            .map(SalesOrderResponse::fromEntity);
    }

    private void validateStateTransition(SalesOrderStatus current, SalesOrderStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == SalesOrderStatus.CONFIRMED || next == SalesOrderStatus.CANCELLED;
            case CONFIRMED -> next == SalesOrderStatus.FULFILLED || next == SalesOrderStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Invalid status transition from %s to %s", current, next)
            );
        }
    }

    private SalesOrder findSalesOrderForTenant(UUID organizationId, UUID soId) {
        return salesOrderRepository.findByIdAndOrganizationId(soId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", soId));
    }
}
