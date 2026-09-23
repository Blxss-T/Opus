package com.opsflow.suppliers.dto;

import com.opsflow.suppliers.domain.Supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    UUID organizationId,
    String name,
    String contactPerson,
    String email,
    String phone,
    String address,
    String taxNumber,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static SupplierResponse fromEntity(Supplier supplier) {
        return new SupplierResponse(
            supplier.getId(),
            supplier.getOrganization().getId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.getAddress(),
            supplier.getTaxNumber(),
            supplier.isActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}
