package com.opsflow.common.tenancy;

import java.util.UUID;

/**
 * Per-request settings applied as PostgreSQL GUCs for RLS.
 * Always clear in a finally block.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> LOGIN_EMAIL = new ThreadLocal<>();
    private static final ThreadLocal<String> API_KEY_HASH = new ThreadLocal<>();
    private static final ThreadLocal<String> REFRESH_TOKEN_HASH = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return ORGANIZATION_ID.get();
    }

    public static void setLoginEmail(String email) {
        LOGIN_EMAIL.set(email);
    }

    public static String getLoginEmail() {
        return LOGIN_EMAIL.get();
    }

    public static void setApiKeyHash(String hash) {
        API_KEY_HASH.set(hash);
    }

    public static String getApiKeyHash() {
        return API_KEY_HASH.get();
    }

    public static void setRefreshTokenHash(String hash) {
        REFRESH_TOKEN_HASH.set(hash);
    }

    public static String getRefreshTokenHash() {
        return REFRESH_TOKEN_HASH.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
        LOGIN_EMAIL.remove();
        API_KEY_HASH.remove();
        REFRESH_TOKEN_HASH.remove();
    }
}
