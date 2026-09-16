package com.opsflow.inventory.dto;

import com.opsflow.inventory.domain.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustStockRequest(
    @NotNull(message = "Quantity delta is required")
    Integer quantityDelta,

    @NotNull(message = "Movement type is required")
    MovementType movementType,

    @NotBlank(message = "Reason is required")
    String reason
) {}
