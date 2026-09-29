package com.opsflow.sales.dto;

import com.opsflow.sales.domain.SalesOrder;
import com.opsflow.sales.domain.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesOrderResponse(
    UUID id,
    UUID organizationId,
    UUID customerId,
    String customerName,
    String customerEmail,
    String soNumber,
    SalesOrderStatus status,
    BigDecimal totalAmount,
    String notes,
    LocalDate expectedDeliveryDate,
    UUID createdBy,
    String createdByEmail,
    List<SalesOrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public static SalesOrderResponse fromEntity(SalesOrder so) {
        List<SalesOrderItemResponse> itemResponses = so.getItems().stream()
            .map(SalesOrderItemResponse::fromEntity)
            .toList();

        return new SalesOrderResponse(
            so.getId(),
            so.getOrganization().getId(),
            so.getCustomer().getId(),
            so.getCustomer().getName(),
            so.getCustomer().getEmail(),
            so.getSoNumber(),
            so.getStatus(),
            so.getTotalAmount(),
            so.getNotes(),
            so.getExpectedDeliveryDate(),
            so.getCreatedBy(),
            so.getCreatedByEmail(),
            itemResponses,
            so.getCreatedAt(),
            so.getUpdatedAt()
        );
    }
}
