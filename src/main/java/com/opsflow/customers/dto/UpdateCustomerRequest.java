package com.opsflow.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Customer name cannot exceed 200 characters")
    String name,

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    String phone,

    String address,

    String notes
) {}
