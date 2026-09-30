package com.opsflow.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierSpendResponse(
    UUID supplierId,
    String supplierName,
    long orderCount,
    BigDecimal totalSpend
) {
}
