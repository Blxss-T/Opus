package com.opsflow.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderItemRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be non-negative")
    BigDecimal unitPrice
) {}
