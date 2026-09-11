package com.opsflow.auth.controller;

import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.google.dto.GoogleAuthRequest;
import com.opsflow.auth.google.service.GoogleAuthService;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.auth.service.AuthService;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, Google sign-in, and session verification")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register Organization & Admin User", description = "Atomically registers a new organization and creates its initial ORG_ADMIN user.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials and returns a JWT Bearer access token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/google")
    @Operation(summary = "Google Account Login", description = "Verifies a Google ID token and issues an Opus JWT. New users receive a personal organization.")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = googleAuthService.authenticateGoogleUser(request.idToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns profile and organization details of the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
