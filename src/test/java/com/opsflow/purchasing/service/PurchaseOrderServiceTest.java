package com.opsflow.purchasing.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
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
import com.opsflow.users.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Organization organization;
    private Supplier supplier;
    private Product product;
    private UserPrincipal adminPrincipal;
    private UUID orgId;
    private UUID supplierId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        productId = UUID.randomUUID();

        organization = new Organization("Acme Corp", "acme-corp");
        organization.setId(orgId);

        supplier = new Supplier(organization, "Acme Supplier", "Bob", "bob@acme.com", null, null, null);
        supplier.setId(supplierId);

        product = new Product(
            organization,
            "SKU-1",
            "Widget",
            "Desc",
            "Hardware",
            new BigDecimal("50.00"),
            new BigDecimal("20.00"),
            10,
            5
        );
        product.setId(productId);

        adminPrincipal = new UserPrincipal(
            UUID.randomUUID(),
            orgId,
            "admin@acme.com",
            "pass",
            Role.ORG_ADMIN,
            true
        );
    }

    @Test
    void shouldCreatePurchaseOrderWithCalculatedTotal() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(productId, 5, new BigDecimal("20.00"));
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
            supplierId,
            LocalDate.now().plusDays(7),
            "Urgent restock",
            List.of(itemReq)
        );

        when(supplierRepository.findByIdAndOrganizationIdAndActiveTrue(supplierId, orgId)).thenReturn(Optional.of(supplier));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(purchaseOrderRepository.countByOrganizationId(orgId)).thenReturn(0L);
        when(productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, orgId)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(adminPrincipal, request);

        assertNotNull(response);
        assertEquals(PurchaseOrderStatus.DRAFT, response.status());
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("100.00"), response.items().get(0).subtotal());
    }

    @Test
    void shouldReplenishInventoryWhenPurchaseOrderIsReceived() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder(
            organization,
            supplier,
            "PO-2026-0001",
            PurchaseOrderStatus.ORDERED,
            new BigDecimal("100.00"),
            "Notes",
            null,
            adminPrincipal.getId(),
            adminPrincipal.getUsername()
        );
        po.setId(poId);

        PurchaseOrderItem item = new PurchaseOrderItem(po, product, 50, new BigDecimal("20.00"), new BigDecimal("1000.00"));
        po.addItem(item);

        when(purchaseOrderRepository.findByIdAndOrganizationId(poId, orgId)).thenReturn(Optional.of(po));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePurchaseOrderStatusRequest statusRequest = new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.RECEIVED);

        PurchaseOrderResponse response = purchaseOrderService.updateStatus(adminPrincipal, poId, statusRequest);

        assertEquals(PurchaseOrderStatus.RECEIVED, response.status());

        // Verify that stock adjustment was triggered for the product
        verify(productService).adjustStock(
            eq(adminPrincipal),
            eq(productId),
            argThat(adj -> adj.quantityDelta() == 50 && adj.movementType() == MovementType.INBOUND)
        );
    }

    @Test
    void shouldThrowExceptionOnInvalidStatusTransition() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder(
            organization,
            supplier,
            "PO-2026-0001",
            PurchaseOrderStatus.DRAFT,
            BigDecimal.ZERO,
            null,
            null,
            adminPrincipal.getId(),
            adminPrincipal.getUsername()
        );
        po.setId(poId);

        when(purchaseOrderRepository.findByIdAndOrganizationId(poId, orgId)).thenReturn(Optional.of(po));

        // Attempting direct jump from DRAFT to RECEIVED
        UpdatePurchaseOrderStatusRequest statusRequest = new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.RECEIVED);

        assertThrows(BusinessException.class, () -> purchaseOrderService.updateStatus(adminPrincipal, poId, statusRequest));
        verify(productService, never()).adjustStock(any(), any(), any());
    }
}
