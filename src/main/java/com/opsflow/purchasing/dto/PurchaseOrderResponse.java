package com.opsflow.purchasing.dto;

import com.opsflow.purchasing.domain.PurchaseOrder;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
    UUID id,
    UUID organizationId,
    UUID supplierId,
    String supplierName,
    String poNumber,
    PurchaseOrderStatus status,
    BigDecimal totalAmount,
    String notes,
    LocalDate expectedDeliveryDate,
    UUID createdBy,
    String createdByEmail,
    List<PurchaseOrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public static PurchaseOrderResponse fromEntity(PurchaseOrder po) {
        List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
            .map(PurchaseOrderItemResponse::fromEntity)
            .toList();

        return new PurchaseOrderResponse(
            po.getId(),
            po.getOrganization().getId(),
            po.getSupplier().getId(),
            po.getSupplier().getName(),
            po.getPoNumber(),
            po.getStatus(),
            po.getTotalAmount(),
            po.getNotes(),
            po.getExpectedDeliveryDate(),
            po.getCreatedBy(),
            po.getCreatedByEmail(),
            itemResponses,
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
    }
}
