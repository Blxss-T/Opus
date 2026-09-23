package com.opsflow.suppliers.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.suppliers.dto.CreateSupplierRequest;
import com.opsflow.suppliers.dto.SupplierResponse;
import com.opsflow.suppliers.dto.UpdateSupplierRequest;
import com.opsflow.suppliers.service.SupplierService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Endpoints for managing vendors and suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Create Supplier", description = "Registers a new vendor/supplier for the caller's organization.")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateSupplierRequest request
    ) {
        SupplierResponse response = supplierService.createSupplier(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "List Suppliers", description = "Retrieves a paginated list of active suppliers for the caller's organization.")
    public ResponseEntity<ApiResponse<Page<SupplierResponse>>> listSuppliers(
        @AuthenticationPrincipal UserPrincipal principal,
        Pageable pageable
    ) {
        Page<SupplierResponse> response = supplierService.listSuppliers(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Supplier Details", description = "Retrieves supplier details by ID.")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        SupplierResponse response = supplierService.getSupplier(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Supplier", description = "Updates supplier contact or business information.")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSupplierRequest request
    ) {
        SupplierResponse response = supplierService.updateSupplier(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Delete Supplier", description = "Soft deletes a supplier.")
    public ResponseEntity<ApiResponse<SupplierResponse>> deleteSupplier(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        SupplierResponse response = supplierService.deleteSupplier(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
