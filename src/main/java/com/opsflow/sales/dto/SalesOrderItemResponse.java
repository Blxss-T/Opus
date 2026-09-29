package com.opsflow.sales.dto;

import com.opsflow.sales.domain.SalesOrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String productSku,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {
    public static SalesOrderItemResponse fromEntity(SalesOrderItem item) {
        return new SalesOrderItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getProduct().getSku(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal()
        );
    }
}
