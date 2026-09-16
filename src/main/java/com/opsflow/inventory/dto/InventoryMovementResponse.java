package com.opsflow.inventory.dto;

import com.opsflow.inventory.domain.InventoryMovement;
import com.opsflow.inventory.domain.MovementType;

import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
    UUID id,
    UUID productId,
    int quantityDelta,
    int previousQuantity,
    int newQuantity,
    MovementType movementType,
    String reason,
    UUID performedBy,
    String performedByEmail,
    Instant createdAt
) {
    public static InventoryMovementResponse fromEntity(InventoryMovement movement) {
        return new InventoryMovementResponse(
            movement.getId(),
            movement.getProduct().getId(),
            movement.getQuantityDelta(),
            movement.getPreviousQuantity(),
            movement.getNewQuantity(),
            movement.getMovementType(),
            movement.getReason(),
            movement.getPerformedBy(),
            movement.getPerformedByEmail(),
            movement.getCreatedAt()
        );
    }
}
