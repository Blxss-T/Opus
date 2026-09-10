package com.opsflow.auth.dto;

import com.opsflow.organizations.domain.Organization;

import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String name,
    String slug,
    boolean active
) {
    public static OrganizationResponse fromEntity(Organization organization) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getName(),
            organization.getSlogan(),
            organization.isActive()
        );
    }
}
