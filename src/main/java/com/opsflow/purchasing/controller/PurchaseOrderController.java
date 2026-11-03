package com.opsflow.purchasing.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.idempotency.IdempotencyKeys;
import com.opsflow.common.idempotency.IdempotentResult;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.purchasing.dto.CreatePurchaseOrderRequest;
import com.opsflow.purchasing.dto.PurchaseOrderResponse;
import com.opsflow.purchasing.dto.UpdatePurchaseOrderStatusRequest;
import com.opsflow.purchasing.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "Purchasing & Purchase Orders", description = "Endpoints for vendor purchasing, PO state transitions, and inventory replenishment")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Create Purchase Order", description = "Drafts a new purchase order with line items.")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = IdempotencyKeys.DESCRIPTION)
        @RequestHeader(value = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
        @Valid @RequestBody CreatePurchaseOrderRequest request
    ) {
        IdempotentResult<PurchaseOrderResponse> result = purchaseOrderService.createPurchaseOrder(
            principal, request, IdempotencyKeys.normalize(idempotencyKey));
        return IdempotencyKeys.toResponse(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "List Purchase Orders", description = "Retrieves a paginated list of purchase orders for the caller's organization. Supports `status` (DRAFT, ORDERED, RECEIVED, CANCELLED), `page`, `size`, and `sort`.")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> listPurchaseOrders(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) PurchaseOrderStatus status,
        Pageable pageable
    ) {
        Page<PurchaseOrderResponse> response = purchaseOrderService.listPurchaseOrders(principal, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Purchase Order Details", description = "Retrieves purchase order details by ID.")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrder(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        PurchaseOrderResponse response = purchaseOrderService.getPurchaseOrder(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Draft Purchase Order", description = "Replaces the supplier, notes, expected delivery date and line items of a DRAFT purchase order. Orders in any other status cannot be edited.")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrder(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody CreatePurchaseOrderRequest request
    ) {
        PurchaseOrderResponse response = purchaseOrderService.updatePurchaseOrder(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Purchase Order Status", description = "Transitions PO status (e.g. DRAFT -> ORDERED -> RECEIVED). Receiving automatically replenishes inventory.")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdatePurchaseOrderStatusRequest request
    ) {
        PurchaseOrderResponse response = purchaseOrderService.updateStatus(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
