package com.opsflow.purchasing.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.inventory.service.ProductService;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.purchasing.domain.PurchaseOrder;
import com.opsflow.purchasing.domain.PurchaseOrderItem;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.purchasing.dto.CreatePurchaseOrderRequest;
import com.opsflow.purchasing.dto.PurchaseOrderItemRequest;
import com.opsflow.purchasing.dto.PurchaseOrderResponse;
import com.opsflow.purchasing.dto.UpdatePurchaseOrderStatusRequest;
import com.opsflow.purchasing.repository.PurchaseOrderRepository;
import com.opsflow.suppliers.domain.Supplier;
import com.opsflow.suppliers.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrganizationRepository organizationRepository;

    public PurchaseOrderService(
        PurchaseOrderRepository purchaseOrderRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        ProductService productService,
        OrganizationRepository organizationRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(UserPrincipal principal, CreatePurchaseOrderRequest request) {
        UUID organizationId = principal.getOrganizationId();

        Supplier supplier = supplierRepository.findByIdAndOrganizationIdAndActiveTrue(request.supplierId(), organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.supplierId()));

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        long count = purchaseOrderRepository.countByOrganizationId(organizationId);
        String poNumber = String.format("PO-%d-%04d", LocalDate.now().getYear(), count + 1);

        PurchaseOrder po = new PurchaseOrder(
            organization,
            supplier,
            poNumber,
            PurchaseOrderStatus.DRAFT,
            BigDecimal.ZERO,
            request.notes(),
            request.expectedDeliveryDate(),
            principal.getId(),
            principal.getUsername()
        );

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findByIdAndOrganizationIdAndActiveTrue(itemReq.productId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.productId()));

            BigDecimal subtotal = itemReq.unitCost().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(subtotal);

            PurchaseOrderItem item = new PurchaseOrderItem(
                po,
                product,
                itemReq.quantity(),
                itemReq.unitCost(),
                subtotal
            );
            po.addItem(item);
        }

        po.setTotalAmount(totalAmount);
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        return PurchaseOrderResponse.fromEntity(savedPo);
    }

    @Transactional
    public PurchaseOrderResponse updateStatus(UserPrincipal principal, UUID poId, UpdatePurchaseOrderStatusRequest request) {
        UUID organizationId = principal.getOrganizationId();
        PurchaseOrder po = findPurchaseOrderForTenant(organizationId, poId);

        PurchaseOrderStatus currentStatus = po.getStatus();
        PurchaseOrderStatus newStatus = request.status();

        if (currentStatus == PurchaseOrderStatus.RECEIVED || currentStatus == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Cannot change status of a %s purchase order", currentStatus)
            );
        }

        validateStateTransition(currentStatus, newStatus);

        if (newStatus == PurchaseOrderStatus.RECEIVED) {
            for (PurchaseOrderItem item : po.getItems()) {
                AdjustStockRequest adjustStockRequest = new AdjustStockRequest(
                    item.getQuantity(),
                    MovementType.INBOUND,
                    String.format("PO %s received from supplier %s", po.getPoNumber(), po.getSupplier().getName())
                );
                productService.adjustStock(principal, item.getProduct().getId(), adjustStockRequest);
            }
        }

        po.setStatus(newStatus);
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        return PurchaseOrderResponse.fromEntity(savedPo);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrder(UserPrincipal principal, UUID poId) {
        PurchaseOrder po = findPurchaseOrderForTenant(principal.getOrganizationId(), poId);
        return PurchaseOrderResponse.fromEntity(po);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> listPurchaseOrders(UserPrincipal principal, Pageable pageable) {
        return purchaseOrderRepository.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId(), pageable)
            .map(PurchaseOrderResponse::fromEntity);
    }

    private void validateStateTransition(PurchaseOrderStatus current, PurchaseOrderStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == PurchaseOrderStatus.ORDERED || next == PurchaseOrderStatus.CANCELLED;
            case ORDERED -> next == PurchaseOrderStatus.RECEIVED || next == PurchaseOrderStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Invalid status transition from %s to %s", current, next)
            );
        }
    }

    private PurchaseOrder findPurchaseOrderForTenant(UUID organizationId, UUID poId) {
        return purchaseOrderRepository.findByIdAndOrganizationId(poId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", poId));
    }
}
