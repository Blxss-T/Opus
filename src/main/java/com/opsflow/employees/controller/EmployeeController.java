package com.opsflow.employees.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.employees.dto.CreateEmployeeRequest;
import com.opsflow.employees.dto.EmployeeResponse;
import com.opsflow.employees.dto.UpdateEmployeeRequest;
import com.opsflow.employees.service.EmployeeService;
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
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Organization-scoped employee records")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','MANAGER')")
    @Operation(summary = "List employees", description = "Returns a paginated list of employees in the caller's organization.")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> list(
        @AuthenticationPrincipal UserPrincipal principal,
        Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.list(principal, pageable)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my employee record", description = "Returns the employee profile linked to the authenticated user.")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getCurrent(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getCurrent(principal)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get employee by id")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(principal, id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(employeeService.create(principal, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','MANAGER')")
    @Operation(summary = "Update employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(principal, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Terminate employee", description = "Marks the employee as TERMINATED. Records are not physically deleted.")
    public ResponseEntity<ApiResponse<EmployeeResponse>> terminate(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.terminate(principal, id)));
    }
}
