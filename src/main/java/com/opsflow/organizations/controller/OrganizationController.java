package com.opsflow.organizations.controller;

import com.opsflow.auth.dto.OrganizationResponse;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.organizations.dto.UpdateOrganizationRequest;
import com.opsflow.organizations.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
@Tag(name = "Organization", description = "The caller's own organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Organization", description = "Returns the organization the caller belongs to.")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getCurrent(principal)));
    }

    @PutMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Update Organization", description = "Renames the caller's organization. The slug does not change.")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateCurrent(principal, request)));
    }
}
