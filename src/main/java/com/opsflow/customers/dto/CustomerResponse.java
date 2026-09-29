package com.opsflow.customers.dto;

import com.opsflow.customers.domain.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    UUID organizationId,
    String name,
    String email,
    String phone,
    String address,
    String notes,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getOrganization().getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getAddress(),
            customer.getNotes(),
            customer.isActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
