package com.opsflow.auth.session.controller;

import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.ChangePasswordRequest;
import com.opsflow.auth.dto.RefreshRequest;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.auth.service.AuthService;
import com.opsflow.auth.session.service.RefreshTokenService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Session Management", description = "Refresh token rotation, logout, and password change endpoints")
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Exchanges a valid refresh token for a new JWT and a rotated refresh token. Reusing a consumed token revokes all sessions for the user.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            AuthResponse response = authService.refreshSession(request.refreshToken());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RefreshTokenService.RevokedRefreshTokenException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, ex.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes all refresh token sessions for the current user.")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change Password", description = "Changes the password after verifying the current one and revokes all existing sessions.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
