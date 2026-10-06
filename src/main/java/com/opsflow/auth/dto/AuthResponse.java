    package com.opsflow.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UserResponse user,
    OrganizationResponse organization
) {
    public static AuthResponse of(String accessToken, String refreshToken, UserResponse user, OrganizationResponse organization) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", user, organization);
    }
}
