package com.opsflow.sales.dto;

import com.opsflow.sales.domain.SalesOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSalesOrderStatusRequest(
    @NotNull(message = "Status is required")
    SalesOrderStatus status
) {}
