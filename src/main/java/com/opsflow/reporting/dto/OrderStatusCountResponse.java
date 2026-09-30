package com.opsflow.reporting.dto;

import com.opsflow.sales.domain.SalesOrderStatus;

public record OrderStatusCountResponse(
    SalesOrderStatus status,
    long orderCount
) {
}
