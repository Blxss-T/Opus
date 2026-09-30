package com.opsflow.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerRevenueResponse(
    UUID customerId,
    String customerName,
    long orderCount,
    BigDecimal totalRevenue
) {
}
