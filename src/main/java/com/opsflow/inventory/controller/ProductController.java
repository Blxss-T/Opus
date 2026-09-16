package com.opsflow.inventory.controller;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.dto.CreateProductRequest;
import com.opsflow.inventory.dto.InventoryMovementResponse;
import com.opsflow.inventory.dto.ProductResponse;
import com.opsflow.inventory.dto.UpdateProductRequest;
import com.opsflow.inventory.service.ProductService;
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
@RequestMapping("/api/v1/products")
@Tag(name = "Inventory & Products", description = "Endpoints for managing products, inventory stock levels, and stock movements")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Create Product", description = "Creates a new product within the caller's organization.")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "List Products", description = "Retrieves a paginated list of active products for the caller's organization.")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> listProducts(
        @AuthenticationPrincipal UserPrincipal principal,
        Pageable pageable
    ) {
        Page<ProductResponse> response = productService.listProducts(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Product Details", description = "Retrieves product details by ID.")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        ProductResponse response = productService.getProduct(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Update Product", description = "Updates pricing, categorization, or reorder parameters of an existing product.")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Delete Product", description = "Soft deletes a product from the organization's catalog.")
    public ResponseEntity<ApiResponse<ProductResponse>> deleteProduct(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        ProductResponse response = productService.deleteProduct(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/adjust-stock")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER')")
    @Operation(summary = "Adjust Stock Level", description = "Applies an atomic inventory quantity delta and logs an audit movement.")
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody AdjustStockRequest request
    ) {
        ProductResponse response = productService.adjustStock(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Get Stock Movements History", description = "Retrieves the historical audit trail of stock movements for a product.")
    public ResponseEntity<ApiResponse<Page<InventoryMovementResponse>>> getMovements(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        Pageable pageable
    ) {
        Page<InventoryMovementResponse> response = productService.getProductMovements(principal, id, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
