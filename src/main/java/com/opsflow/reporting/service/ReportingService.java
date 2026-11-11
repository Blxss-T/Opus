package com.opsflow.reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.cache.OrgCacheService;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.reporting.dto.CategorySalesResponse;
import com.opsflow.reporting.dto.CategorySpendResponse;
import com.opsflow.reporting.dto.CustomerRevenueResponse;
import com.opsflow.reporting.dto.DailySalesResponse;
import com.opsflow.reporting.dto.DashboardSummaryResponse;
import com.opsflow.reporting.dto.LowStockProductResponse;
import com.opsflow.reporting.dto.MonthlyTrendResponse;
import com.opsflow.reporting.dto.OrderStatusCountResponse;
import com.opsflow.reporting.dto.SupplierSpendResponse;
import com.opsflow.reporting.repository.ReportingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReportingService {

    private static final int DEFAULT_TOP_LIMIT = 5;
    private static final int MAX_TOP_LIMIT = 25;

    private static final TypeReference<List<LowStockProductResponse>> LOW_STOCK_TYPE = new TypeReference<>() {};

    private final ReportingRepository reportingRepository;
    private final ProductRepository productRepository;
    private final OrgCacheService orgCacheService;

    public ReportingService(
        ReportingRepository reportingRepository,
        ProductRepository productRepository,
        OrgCacheService orgCacheService
    ) {
        this.reportingRepository = reportingRepository;
        this.productRepository = productRepository;
        this.orgCacheService = orgCacheService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(UserPrincipal principal) {
        UUID organizationId = principal.getOrganizationId();
        if (orgCacheService == null) {
            return loadDashboard(organizationId);
        }
        return orgCacheService.getOrLoad(organizationId, "dashboard", DashboardSummaryResponse.class, () -> loadDashboard(organizationId));
    }

    private DashboardSummaryResponse loadDashboard(UUID organizationId) {
        return new DashboardSummaryResponse(
            reportingRepository.countProducts(organizationId),
            reportingRepository.countLowStockProducts(organizationId),
            reportingRepository.countCustomers(organizationId),
            reportingRepository.countSuppliers(organizationId),
            reportingRepository.countSalesOrders(organizationId),
            reportingRepository.countOpenPurchaseOrders(organizationId),
            reportingRepository.sumSalesRevenue(organizationId),
            reportingRepository.sumPurchaseSpend(organizationId),
            reportingRepository.countMovementsLast30Days(organizationId, MovementType.INBOUND),
            reportingRepository.countMovementsLast30Days(organizationId, MovementType.OUTBOUND),
            reportingRepository.countSalesOrdersByStatus(organizationId)
        );
    }

    @Transactional(readOnly = true)
    public List<CategorySalesResponse> getSalesByCategory(UserPrincipal principal) {
        return reportingRepository.findSalesByCategory(principal.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<CategorySpendResponse> getPurchasesByCategory(UserPrincipal principal) {
        return reportingRepository.findPurchasesByCategory(principal.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<CustomerRevenueResponse> getTopCustomers(UserPrincipal principal, Integer limit) {
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_TOP_LIMIT : Math.min(limit, MAX_TOP_LIMIT);
        return reportingRepository.findTopCustomersByRevenue(principal.getOrganizationId(), effectiveLimit);
    }

    @Transactional(readOnly = true)
    public List<SupplierSpendResponse> getTopSuppliers(UserPrincipal principal, Integer limit) {
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_TOP_LIMIT : Math.min(limit, MAX_TOP_LIMIT);
        return reportingRepository.findTopSuppliersBySpend(principal.getOrganizationId(), effectiveLimit);
    }

    @Transactional(readOnly = true)
    public List<DailySalesResponse> getDailySales(UserPrincipal principal) {
        return reportingRepository.findDailySalesLast30Days(principal.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrend(UserPrincipal principal) {
        return reportingRepository.findMonthlySalesTrend(principal.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts(UserPrincipal principal) {
        UUID organizationId = principal.getOrganizationId();
        java.util.function.Supplier<List<LowStockProductResponse>> loader = () ->
            productRepository.findByOrganizationIdAndLowStock(organizationId)
                .stream()
                .map(LowStockProductResponse::fromEntity)
                .toList();
        if (orgCacheService == null) {
            return loader.get();
        }
        return orgCacheService.getOrLoad(organizationId, "low-stock", LOW_STOCK_TYPE, loader);
    }
}
