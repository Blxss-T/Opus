package com.opsflow.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
    @NotNull(message = "Supplier ID is required")
    UUID supplierId,

    LocalDate expectedDeliveryDate,

    String notes,

    @NotEmpty(message = "Purchase order must contain at least one line item")
    @Valid
    List<PurchaseOrderItemRequest> items
) {}
