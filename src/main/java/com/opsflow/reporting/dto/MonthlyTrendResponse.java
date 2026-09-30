package com.opsflow.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyTrendResponse(
    LocalDate month,
    BigDecimal totalSales
) {
}
