package com.opsflow.users.controller;

import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.users.domain.Role;
import com.opsflow.users.dto.CreateUserRequest;
import com.opsflow.users.dto.UpdateUserRequest;
import com.opsflow.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Endpoints for managing user accounts within the caller's organization")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create User", description = "Creates a MANAGER or EMPLOYEE account in the caller's organization with an initial password.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.createUser(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "List Users", description = "Retrieves a paginated list of users for the caller's organization. Supports `q` (case-insensitive match on first name, last name, or email), `role`, `active`, `page`, `size`, and `sort`.")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) Boolean active,
        Pageable pageable
    ) {
        Page<UserResponse> response = userService.listUsers(principal, q, role, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Get User Details", description = "Retrieves a user by ID within the caller's organization.")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        UserResponse response = userService.getUser(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Update User", description = "Updates a user's name or role (MANAGER or EMPLOYEE). The ORG_ADMIN role cannot be changed.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Deactivate User", description = "Deactivates a user and revokes all of their sessions.")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        UserResponse response = userService.deactivateUser(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Activate User", description = "Reactivates a previously deactivated user.")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        UserResponse response = userService.activateUser(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
