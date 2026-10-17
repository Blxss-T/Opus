package com.opsflow.apikeys.domain;

import com.opsflow.common.domain.BaseEntity;
import com.opsflow.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64, unique = true)
    private String keyHash;

    /** Comma-separated {@link ApiKeyScope} values, e.g. "products:read,reports:read". */
    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_by_email", nullable = false)
    private String createdByEmail;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public ApiKey() {
    }

    public ApiKey(
        Organization organization,
        String name,
        String keyPrefix,
        String keyHash,
        Set<ApiKeyScope> scopes,
        UUID createdBy,
        String createdByEmail,
        Instant expiresAt
    ) {
        this.organization = organization;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.scopes = new TreeSet<>(scopes).stream().map(ApiKeyScope::getValue).collect(Collectors.joining(","));
        this.createdBy = createdBy;
        this.createdByEmail = createdByEmail;
        this.expiresAt = expiresAt;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public Set<ApiKeyScope> getScopes() {
        return Arrays.stream(scopes.split(","))
            .map(ApiKeyScope::fromValue)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    /** Usable right now: not revoked, not expired, and the organization is active. */
    public boolean isUsable(Instant now) {
        return revokedAt == null
            && (expiresAt == null || expiresAt.isAfter(now))
            && organization.isActive();
    }
}
