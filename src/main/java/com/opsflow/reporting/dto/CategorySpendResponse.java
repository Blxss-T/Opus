package com.opsflow.reporting.dto;

import java.math.BigDecimal;

public record CategorySpendResponse(
    String category,
    BigDecimal totalSpend
) {
}
