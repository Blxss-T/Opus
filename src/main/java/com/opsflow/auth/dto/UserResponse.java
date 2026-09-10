package com.opsflow.auth.dto;

import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;

import java.util.UUID;

public record UserResponse(
    UUID id,
    UUID organizationId,
    String email,
    String firstName,
    String lastName,
    Role role,
    boolean active
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getOrganization().getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.isActive()
        );
    }
}
