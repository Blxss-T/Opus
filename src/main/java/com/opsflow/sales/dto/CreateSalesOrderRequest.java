package com.opsflow.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSalesOrderRequest(
    @NotNull(message = "Customer ID is required")
    UUID customerId,

    LocalDate expectedDeliveryDate,

    String notes,

    @NotEmpty(message = "Sales order must contain at least one line item")
    @Valid
    List<SalesOrderItemRequest> items
) {}
