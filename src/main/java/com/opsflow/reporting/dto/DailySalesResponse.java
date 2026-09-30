package com.opsflow.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesResponse(
    LocalDate saleDate,
    BigDecimal totalSales
) {
}
