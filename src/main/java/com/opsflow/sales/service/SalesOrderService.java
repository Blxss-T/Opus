package com.opsflow.sales.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.common.idempotency.IdempotentResult;
import com.opsflow.common.jpa.FilterSpecs;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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
        return createSalesOrder(principal, request, null).value();
    }

    /**
     * Creates the order, or returns the order already created with the same
     * idempotency key so a client can safely retry a request whose response
     * it never received.
     */
    @Transactional
    public IdempotentResult<SalesOrderResponse> createSalesOrder(UserPrincipal principal, CreateSalesOrderRequest request, String idempotencyKey) {
        UUID organizationId = principal.getOrganizationId();

        Customer customer = customerRepository.findByIdAndOrganizationIdAndActiveTrue(request.customerId(), organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));

        // Row lock serializes number generation per organization so concurrent
        // creates cannot both read the same count.
        Organization organization = organizationRepository.findByIdForUpdate(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        // Checked under the organization lock, so two concurrent requests with
        // the same key cannot both create an order.
        if (idempotencyKey != null) {
            Optional<SalesOrder> existing = salesOrderRepository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey);
            if (existing.isPresent()) {
                return IdempotentResult.replayed(SalesOrderResponse.fromEntity(existing.get()));
            }
        }

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
        so.setIdempotencyKey(idempotencyKey);
        SalesOrder savedSo = salesOrderRepository.save(so);

        return IdempotentResult.created(SalesOrderResponse.fromEntity(savedSo));
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
                productService.adjustStockForOrder(principal, item.getProduct().getId(), adjustStockRequest);
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
    public Page<SalesOrderResponse> listSalesOrders(UserPrincipal principal, SalesOrderStatus status, Pageable pageable) {
        Specification<SalesOrder> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.equalsValue("status", status)
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.desc("createdAt"));
        return salesOrderRepository.findAll(spec, effective).map(SalesOrderResponse::fromEntity);
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
