package com.opsflow.purchasing.dto;

import com.opsflow.purchasing.domain.PurchaseOrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String productSku,
    int quantity,
    BigDecimal unitCost,
    BigDecimal subtotal
) {
    public static PurchaseOrderItemResponse fromEntity(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getSku(),
            item.getQuantity(),
            item.getUnitCost(),
            item.getSubtotal()
        );
    }
}
