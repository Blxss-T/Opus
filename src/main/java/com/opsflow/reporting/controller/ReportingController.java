package com.opsflow.reporting.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.reporting.dto.CategorySalesResponse;
import com.opsflow.reporting.dto.CategorySpendResponse;
import com.opsflow.reporting.dto.CustomerRevenueResponse;
import com.opsflow.reporting.dto.DailySalesResponse;
import com.opsflow.reporting.dto.DashboardSummaryResponse;
import com.opsflow.reporting.dto.LowStockProductResponse;
import com.opsflow.reporting.dto.MonthlyTrendResponse;
import com.opsflow.reporting.dto.SupplierSpendResponse;
import com.opsflow.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reporting & Dashboard", description = "Aggregated read-only metrics across inventory, sales, purchasing, customers, and suppliers")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Dashboard Summary", description = "Aggregate counters: products, low stock, customers, suppliers, orders, revenue, spend, and recent movement counts.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getDashboardSummary(principal)));
    }

    @GetMapping("/dashboard/low-stock")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Low Stock Alerts", description = "Active products at or below their reorder level, most urgent first.")
    public ResponseEntity<ApiResponse<List<LowStockProductResponse>>> getLowStockProducts(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getLowStockProducts(principal)));
    }

    @GetMapping("/sales/by-category")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Sales by Category", description = "Fulfilled sales revenue grouped by product category.")
    public ResponseEntity<ApiResponse<List<CategorySalesResponse>>> getSalesByCategory(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getSalesByCategory(principal)));
    }

    @GetMapping("/purchases/by-category")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Purchases by Category", description = "Received purchase spend grouped by product category.")
    public ResponseEntity<ApiResponse<List<CategorySpendResponse>>> getPurchasesByCategory(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getPurchasesByCategory(principal)));
    }

    @GetMapping("/sales/top-customers")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Top Customers", description = "Customers ranked by fulfilled sales revenue. limit defaults to 5, max 25.")
    public ResponseEntity<ApiResponse<List<CustomerRevenueResponse>>> getTopCustomers(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getTopCustomers(principal, limit)));
    }

    @GetMapping("/purchases/top-suppliers")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Top Suppliers", description = "Suppliers ranked by received purchase spend. limit defaults to 5, max 25.")
    public ResponseEntity<ApiResponse<List<SupplierSpendResponse>>> getTopSuppliers(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getTopSuppliers(principal, limit)));
    }

    @GetMapping("/sales/daily")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Daily Sales (30 days)", description = "Fulfilled sales revenue grouped by day for the last 30 days.")
    public ResponseEntity<ApiResponse<List<DailySalesResponse>>> getDailySales(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getDailySales(principal)));
    }

    @GetMapping("/sales/monthly-trend")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Monthly Sales Trend", description = "Fulfilled sales revenue grouped by month.")
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getMonthlyTrend(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getMonthlyTrend(principal)));
    }
}
