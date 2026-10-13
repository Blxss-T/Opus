package com.opsflow.apikeys.controller;

import com.opsflow.apikeys.domain.ApiKeyScope;
import com.opsflow.apikeys.dto.ApiKeyResponse;
import com.opsflow.apikeys.dto.CreateApiKeyRequest;
import com.opsflow.apikeys.dto.CreatedApiKeyResponse;
import com.opsflow.apikeys.service.ApiKeyService;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@Tag(name = "API Keys", description = "Keys that let other applications call the API on behalf of the organization. Managed by the ORG_ADMIN with a signed-in session; an API key cannot manage keys.")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create API Key", description = "Creates a key with the given scopes. The raw key is returned in `data.key` once and cannot be retrieved again. Send it on requests as the `X-API-Key` header.")
    public ResponseEntity<ApiResponse<CreatedApiKeyResponse>> createApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        CreatedApiKeyResponse response = apiKeyService.createApiKey(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "List API Keys", description = "Lists the organization's keys, newest first, including revoked and expired ones. Only the key prefix is shown.")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.listApiKeys(principal)));
    }

    @GetMapping("/scopes")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "List Available Scopes", description = "Scopes that can be granted to a key. A `:write` scope also allows reads of the same resource.")
    public ResponseEntity<ApiResponse<List<ApiKeyScope>>> listScopes() {
        return ResponseEntity.ok(ApiResponse.success(List.of(ApiKeyScope.values())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Revoke API Key", description = "Revokes a key immediately. Revocation cannot be undone; create a new key instead.")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revokeApiKey(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.revokeApiKey(principal, id)));
    }
}
