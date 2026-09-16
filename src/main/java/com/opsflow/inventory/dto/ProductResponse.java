package com.opsflow.inventory.dto;

import com.opsflow.inventory.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID organizationId,
    String sku,
    String name,
    String description,
    String category,
    BigDecimal unitPrice,
    BigDecimal costPrice,
    int stockQuantity,
    int reorderLevel,
    boolean active,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getOrganization().getId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getCategory(),
            product.getUnitPrice(),
            product.getCostPrice(),
            product.getStockQuantity(),
            product.getReorderLevel(),
            product.isActive(),
            product.getVersion(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
