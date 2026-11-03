package com.opsflow.apikeys.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * What an API key may do. A scope is "resource:read" or "resource:write";
 * write includes read. Resources without a scope here (users, API keys,
 * auth, creating or terminating employees) are not available to API keys.
 */
public enum ApiKeyScope {

    PRODUCTS_READ("products", false),
    PRODUCTS_WRITE("products", true),
    CUSTOMERS_READ("customers", false),
    CUSTOMERS_WRITE("customers", true),
    SUPPLIERS_READ("suppliers", false),
    SUPPLIERS_WRITE("suppliers", true),
    PURCHASE_ORDERS_READ("purchase-orders", false),
    PURCHASE_ORDERS_WRITE("purchase-orders", true),
    SALES_ORDERS_READ("sales-orders", false),
    SALES_ORDERS_WRITE("sales-orders", true),
    EMPLOYEES_READ("employees", false),
    REPORTS_READ("reports", false);

    private static final String API_PREFIX = "/api/v1/";

    private final String resource;
    private final boolean write;

    ApiKeyScope(String resource, boolean write) {
        this.resource = resource;
        this.write = write;
    }

    @JsonValue
    public String getValue() {
        return resource + (write ? ":write" : ":read");
    }

    /** True when holding this scope is enough for a request that needs {@code required}. */
    public boolean satisfies(ApiKeyScope required) {
        return resource.equals(required.resource) && (write || !required.write);
    }

    @JsonCreator
    public static ApiKeyScope fromValue(String value) {
        return Arrays.stream(values())
            .filter(scope -> scope.getValue().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown API key scope: " + value));
    }

    /**
     * The scope a request needs, or empty when the endpoint is not available
     * to API keys at all.
     */
    public static Optional<ApiKeyScope> requiredFor(String method, String path) {
        if (path == null || !path.startsWith(API_PREFIX)) {
            return Optional.empty();
        }
        String rest = path.substring(API_PREFIX.length());
        // Tied to the employee record of a signed-in person.
        if (rest.equals("employees/me")) {
            return Optional.empty();
        }

        int slash = rest.indexOf('/');
        String resource = slash < 0 ? rest : rest.substring(0, slash);
        boolean write = !("GET".equals(method) || "HEAD".equals(method));

        return Arrays.stream(values())
            .filter(scope -> scope.resource.equals(resource) && scope.write == write)
            .findFirst();
    }
}
