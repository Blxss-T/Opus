package com.opsflow.customers.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.customers.dto.CreateCustomerRequest;
import com.opsflow.customers.dto.CustomerResponse;
import com.opsflow.customers.dto.UpdateCustomerRequest;
import com.opsflow.customers.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Endpoints for managing organization customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Create Customer", description = "Registers a new customer for the caller's organization.")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponse response = customerService.createCustomer(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "List Customers", description = "Retrieves a paginated list of active customers for the caller's organization. Supports `q` (case-insensitive match on name or email), `page`, `size`, and `sort`.")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> listCustomers(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String q,
        Pageable pageable
    ) {
        Page<CustomerResponse> response = customerService.listCustomers(principal, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Customer Details", description = "Retrieves customer details by ID within the caller's organization.")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        CustomerResponse response = customerService.getCustomer(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Customer", description = "Updates customer contact or notes.")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponse response = customerService.updateCustomer(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Delete Customer", description = "Soft deletes a customer.")
    public ResponseEntity<ApiResponse<CustomerResponse>> deleteCustomer(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        CustomerResponse response = customerService.deleteCustomer(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
