    package com.opsflow.auth.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserResponse user,
    OrganizationResponse organization
) {
    public static AuthResponse of(String accessToken, UserResponse user, OrganizationResponse organization) {
        return new AuthResponse(accessToken, "Bearer", user, organization);
    }
}
