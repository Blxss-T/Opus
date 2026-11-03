package com.opsflow.audit.controller;

import com.opsflow.audit.domain.AuditActorType;
import com.opsflow.audit.dto.AuditEventResponse;
import com.opsflow.audit.service.AuditService;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-log")
@Tag(name = "Audit Log", description = "Record of successful changes made by users and API keys in the caller's organization")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "List Audit Events", description = "Newest first. Supports `actor` (case-insensitive match on the user's email or `api-key:<name>`), `actorType` (USER, API_KEY), `resourceType` (e.g. `products`, `sales-orders`), `resourceId`, `page`, `size`, and `sort`.")
    public ResponseEntity<ApiResponse<Page<AuditEventResponse>>> listAuditEvents(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String actor,
        @RequestParam(required = false) AuditActorType actorType,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) UUID resourceId,
        Pageable pageable
    ) {
        Page<AuditEventResponse> response = auditService.list(principal, actor, actorType, resourceType, resourceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
