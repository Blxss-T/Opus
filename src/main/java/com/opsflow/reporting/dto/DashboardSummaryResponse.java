package com.opsflow.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    long activeProducts,
    long lowStockProducts,
    long activeCustomers,
    long activeSuppliers,
    long totalSalesOrders,
    long openPurchaseOrders,
    BigDecimal totalSalesRevenue,
    BigDecimal totalPurchaseSpend,
    long inboundMovementsLast30Days,
    long outboundMovementsLast30Days,
    List<OrderStatusCountResponse> salesOrdersByStatus
) {
}
