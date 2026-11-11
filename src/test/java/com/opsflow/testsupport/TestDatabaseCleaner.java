package com.opsflow.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deletes all rows from every table in reverse dependency order so that
 * {@code @SpringBootTest} classes sharing the single H2 in-memory test
 * database can safely reset state in their {@code setUp} methods without
 * hitting foreign key violations.
 */
@Component
public class TestDatabaseCleaner {

    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
        "ops.audit_events",
        "identity.refresh_tokens",
        "ops.api_keys",
        "sales.sales_order_items",
        "sales.sales_orders",
        "purchasing.purchase_order_items",
        "purchasing.purchase_orders",
        "catalog.inventory_movements",
        "catalog.products",
        "sales.customers",
        "purchasing.suppliers",
        "identity.employees",
        "identity.otp_codes",
        "identity.users",
        "identity.organizations"
    );

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        for (String table : TABLES_IN_DELETE_ORDER) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }
    }
}
