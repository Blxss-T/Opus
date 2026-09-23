package com.opsflow.purchasing.dto;

import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePurchaseOrderStatusRequest(
    @NotNull(message = "Status is required")
    PurchaseOrderStatus status
) {}
