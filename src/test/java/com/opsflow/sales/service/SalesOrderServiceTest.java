package com.opsflow.sales.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private Organization organization;
    private Customer customer;
    private Product product;
    private UserPrincipal adminPrincipal;
    private UUID orgId;
    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        organization = new Organization("Acme Corp", "acme-corp");
        organization.setId(orgId);

        customer = new Customer(organization, "Acme Customer", "cust@acme.com", null, null, null);
        customer.setId(customerId);

        product = new Product(
            organization,
            "SKU-1",
            "Widget",
            "Desc",
            "Hardware",
            new BigDecimal("50.00"),
            new BigDecimal("20.00"),
            100,
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
    void shouldCreateSalesOrderWithCalculatedTotal() {
        SalesOrderItemRequest itemReq = new SalesOrderItemRequest(productId, 5, new BigDecimal("50.00"));
        CreateSalesOrderRequest request = new CreateSalesOrderRequest(
            customerId,
            LocalDate.now().plusDays(7),
            "Urgent order",
            List.of(itemReq)
        );

        when(customerRepository.findByIdAndOrganizationIdAndActiveTrue(customerId, orgId)).thenReturn(Optional.of(customer));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(salesOrderRepository.countByOrganizationId(orgId)).thenReturn(0L);
        when(productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, orgId)).thenReturn(Optional.of(product));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        SalesOrderResponse response = salesOrderService.createSalesOrder(adminPrincipal, request);

        assertNotNull(response);
        assertEquals(SalesOrderStatus.DRAFT, response.status());
        assertEquals(new BigDecimal("250.00"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("250.00"), response.items().get(0).subtotal());
    }

    @Test
    void shouldDepleteInventoryWhenSalesOrderIsFulfilled() {
        UUID soId = UUID.randomUUID();
        SalesOrder so = new SalesOrder(
            organization,
            customer,
            "SO-2026-0001",
            SalesOrderStatus.CONFIRMED,
            new BigDecimal("250.00"),
            "Notes",
            null,
            adminPrincipal.getId(),
            adminPrincipal.getUsername()
        );
        so.setId(soId);

        SalesOrderItem item = new SalesOrderItem(so, product, 30, new BigDecimal("50.00"), new BigDecimal("1500.00"));
        so.addItem(item);

        when(salesOrderRepository.findByIdAndOrganizationId(soId, orgId)).thenReturn(Optional.of(so));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSalesOrderStatusRequest statusRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.FULFILLED);

        SalesOrderResponse response = salesOrderService.updateStatus(adminPrincipal, soId, statusRequest);

        assertEquals(SalesOrderStatus.FULFILLED, response.status());

        // Verify that stock adjustment was triggered for the product with a negative delta (OUTBOUND)
        verify(productService).adjustStock(
            eq(adminPrincipal),
            eq(productId),
            argThat(adj -> adj.quantityDelta() == -30 && adj.movementType() == MovementType.OUTBOUND)
        );
    }

    @Test
    void shouldThrowExceptionOnInvalidStatusTransition() {
        UUID soId = UUID.randomUUID();
        SalesOrder so = new SalesOrder(
            organization,
            customer,
            "SO-2026-0001",
            SalesOrderStatus.DRAFT,
            BigDecimal.ZERO,
            null,
            null,
            adminPrincipal.getId(),
            adminPrincipal.getUsername()
        );
        so.setId(soId);

        when(salesOrderRepository.findByIdAndOrganizationId(soId, orgId)).thenReturn(Optional.of(so));

        // Attempting direct jump from DRAFT to FULFILLED
        UpdateSalesOrderStatusRequest statusRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.FULFILLED);

        assertThrows(BusinessException.class, () -> salesOrderService.updateStatus(adminPrincipal, soId, statusRequest));
        verify(productService, never()).adjustStock(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenChangingTerminalStatus() {
        UUID soId = UUID.randomUUID();
        SalesOrder so = new SalesOrder(
            organization,
            customer,
            "SO-2026-0001",
            SalesOrderStatus.CANCELLED,
            BigDecimal.ZERO,
            null,
            null,
            adminPrincipal.getId(),
            adminPrincipal.getUsername()
        );
        so.setId(soId);

        when(salesOrderRepository.findByIdAndOrganizationId(soId, orgId)).thenReturn(Optional.of(so));

        UpdateSalesOrderStatusRequest statusRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.CONFIRMED);

        assertThrows(BusinessException.class, () -> salesOrderService.updateStatus(adminPrincipal, soId, statusRequest));
        verify(productService, never()).adjustStock(any(), any(), any());
    }
}
