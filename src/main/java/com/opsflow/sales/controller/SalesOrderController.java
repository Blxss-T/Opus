package com.opsflow.sales.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.sales.dto.CreateSalesOrderRequest;
import com.opsflow.sales.dto.SalesOrderResponse;
import com.opsflow.sales.dto.UpdateSalesOrderStatusRequest;
import com.opsflow.sales.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales-orders")
@Tag(name = "Sales & Sales Orders", description = "Endpoints for customer sales, SO state transitions, and inventory depletion")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Create Sales Order", description = "Drafts a new sales order with line items for a customer.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> createSalesOrder(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateSalesOrderRequest request
    ) {
        SalesOrderResponse response = salesOrderService.createSalesOrder(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "List Sales Orders", description = "Retrieves a paginated list of sales orders for the caller's organization.")
    public ResponseEntity<ApiResponse<Page<SalesOrderResponse>>> listSalesOrders(
        @AuthenticationPrincipal UserPrincipal principal,
        Pageable pageable
    ) {
        Page<SalesOrderResponse> response = salesOrderService.listSalesOrders(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Sales Order Details", description = "Retrieves sales order details by ID.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> getSalesOrder(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        SalesOrderResponse response = salesOrderService.getSalesOrder(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Sales Order Status", description = "Transitions SO status (e.g. DRAFT -> CONFIRMED -> FULFILLED). Fulfilling automatically depletes inventory.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> updateStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSalesOrderStatusRequest request
    ) {
        SalesOrderResponse response = salesOrderService.updateStatus(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
