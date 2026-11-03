package com.opsflow.organizations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Organization name must be at most 255 characters")
    String name
) {}
