package com.opsflow.reporting.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.inventory.domain.Product;
import com.opsflow.reporting.dto.CategorySalesResponse;
import com.opsflow.reporting.dto.CustomerRevenueResponse;
import com.opsflow.reporting.dto.DashboardSummaryResponse;
import com.opsflow.reporting.dto.LowStockProductResponse;
import com.opsflow.reporting.repository.ReportingRepository;
import com.opsflow.sales.domain.SalesOrderStatus;
import com.opsflow.users.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportingRepository reportingRepository;

    @Mock
    private com.opsflow.inventory.repository.ProductRepository productRepository;

    @InjectMocks
    private ReportingService reportingService;

    private UserPrincipal adminPrincipal;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
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
    void shouldAggregateDashboardSummaryForTenant() {
        when(reportingRepository.countProducts(orgId)).thenReturn(10L);
        when(reportingRepository.countLowStockProducts(orgId)).thenReturn(2L);
        when(reportingRepository.countCustomers(orgId)).thenReturn(7L);
        when(reportingRepository.countSuppliers(orgId)).thenReturn(3L);
        when(reportingRepository.countSalesOrders(orgId)).thenReturn(25L);
        when(reportingRepository.countOpenPurchaseOrders(orgId)).thenReturn(4L);
        when(reportingRepository.sumSalesRevenue(orgId)).thenReturn(new BigDecimal("5000.00"));
        when(reportingRepository.sumPurchaseSpend(orgId)).thenReturn(new BigDecimal("3000.00"));
        when(reportingRepository.countMovementsLast30Days(orgId, com.opsflow.inventory.domain.MovementType.INBOUND)).thenReturn(6L);
        when(reportingRepository.countMovementsLast30Days(orgId, com.opsflow.inventory.domain.MovementType.OUTBOUND)).thenReturn(9L);
        when(reportingRepository.countSalesOrdersByStatus(orgId)).thenReturn(
            List.of(new com.opsflow.reporting.dto.OrderStatusCountResponse(SalesOrderStatus.FULFILLED, 12L))
        );

        DashboardSummaryResponse summary = reportingService.getDashboardSummary(adminPrincipal);

        assertEquals(10L, summary.activeProducts());
        assertEquals(2L, summary.lowStockProducts());
        assertEquals(7L, summary.activeCustomers());
        assertEquals(3L, summary.activeSuppliers());
        assertEquals(25L, summary.totalSalesOrders());
        assertEquals(4L, summary.openPurchaseOrders());
        assertEquals(new BigDecimal("5000.00"), summary.totalSalesRevenue());
        assertEquals(new BigDecimal("3000.00"), summary.totalPurchaseSpend());
        assertEquals(6L, summary.inboundMovementsLast30Days());
        assertEquals(9L, summary.outboundMovementsLast30Days());
        assertEquals(1, summary.salesOrdersByStatus().size());
        assertEquals(SalesOrderStatus.FULFILLED, summary.salesOrdersByStatus().get(0).status());
    }

    @Test
    void shouldDefaultTopCustomersLimitWhenNull() {
        when(reportingRepository.findTopCustomersByRevenue(orgId, 5)).thenReturn(List.of());

        reportingService.getTopCustomers(adminPrincipal, null);

        verify(reportingRepository).findTopCustomersByRevenue(orgId, 5);
    }

    @Test
    void shouldClampTopCustomersLimitTo25() {
        when(reportingRepository.findTopCustomersByRevenue(orgId, 25)).thenReturn(List.of());

        reportingService.getTopCustomers(adminPrincipal, 500);

        verify(reportingRepository).findTopCustomersByRevenue(orgId, 25);
    }

    @Test
    void shouldDefaultTopSuppliersLimitWhenNegative() {
        when(reportingRepository.findTopSuppliersBySpend(orgId, 5)).thenReturn(List.of());

        reportingService.getTopSuppliers(adminPrincipal, -3);

        verify(reportingRepository).findTopSuppliersBySpend(orgId, 5);
    }

    @Test
    void shouldMapLowStockProductsToResponses() {
        Product product = new Product(
            null,
            "SKU-LOW",
            "Low Stock Widget",
            null,
            "Hardware",
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            3,
            10
        );
        product.setId(UUID.randomUUID());

        when(productRepository.findByOrganizationIdAndLowStock(orgId)).thenReturn(List.of(product));

        List<LowStockProductResponse> result = reportingService.getLowStockProducts(adminPrincipal);

        assertEquals(1, result.size());
        assertEquals("SKU-LOW", result.get(0).sku());
        assertEquals(3, result.get(0).stockQuantity());
        assertEquals(10, result.get(0).reorderLevel());
        assertEquals(product.getId(), result.get(0).productId());
    }

    @Test
    void shouldReturnSalesByCategoryForTenant() {
        when(reportingRepository.findSalesByCategory(orgId)).thenReturn(
            List.of(new CategorySalesResponse("Hardware", new BigDecimal("1250.00")))
        );

        var result = reportingService.getSalesByCategory(adminPrincipal);

        assertEquals(1, result.size());
        assertEquals("Hardware", result.get(0).category());
        assertEquals(new BigDecimal("1250.00"), result.get(0).totalSales());
    }

    @Test
    void shouldNotInteractWithRepositoriesWhenNothingToReport() {
        when(reportingRepository.findTopCustomersByRevenue(orgId, 5)).thenReturn(List.of());

        List<CustomerRevenueResponse> result = reportingService.getTopCustomers(adminPrincipal, 0);

        assertEquals(0, result.size());
        verifyNoInteractions(productRepository);
    }
}
