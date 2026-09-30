package com.opsflow.reporting.dto;

import java.math.BigDecimal;

public record CategorySalesResponse(
    String category,
    BigDecimal totalSales
) {
}
