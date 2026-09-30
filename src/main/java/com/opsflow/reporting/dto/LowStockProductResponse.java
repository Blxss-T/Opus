package com.opsflow.reporting.dto;

import java.util.UUID;

public record LowStockProductResponse(
    UUID productId,
    String sku,
    String name,
    String category,
    int stockQuantity,
    int reorderLevel
) {
    public static LowStockProductResponse fromEntity(com.opsflow.inventory.domain.Product product) {
        return new LowStockProductResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getStockQuantity(),
            product.getReorderLevel()
        );
    }
}
