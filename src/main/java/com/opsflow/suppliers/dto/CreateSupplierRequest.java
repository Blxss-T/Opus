package com.opsflow.suppliers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
    @NotBlank(message = "Supplier name is required")
    @Size(max = 200, message = "Supplier name cannot exceed 200 characters")
    String name,

    @Size(max = 100, message = "Contact person cannot exceed 100 characters")
    String contactPerson,

    @Email(message = "Invalid email format")
    String email,

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    String phone,

    String address,

    @Size(max = 100, message = "Tax number cannot exceed 100 characters")
    String taxNumber
) {}
