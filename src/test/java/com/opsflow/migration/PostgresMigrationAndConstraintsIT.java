package com.opsflow.migration;

import com.opsflow.common.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("it")
@Tag("integration")
@EnabledIf("isDockerAvailable")
public class PostgresMigrationAndConstraintsIT {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("opus_db")
        .withUsername("opus_user")
        .withPassword("opus_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (isDockerAvailable()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void shouldExecuteMigrationsAndEnforceSingleOrgAdminPartialUniqueIndex() {
        UUID orgId = UUID.randomUUID();
        setOrg(orgId);
        jdbcTemplate.update(
            "INSERT INTO identity.organizations (id, name, slug, active, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            orgId, "Acme Test Corp", "acme-" + orgId, true
        );

        UUID admin1Id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO identity.users (id, organization_id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
            admin1Id, orgId, "admin1@acme.test", "hash1", "Admin", "One", "ORG_ADMIN", true
        );

        UUID employeeId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO identity.users (id, organization_id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
            employeeId, orgId, "employee@acme.test", "hash2", "Emp", "One", "EMPLOYEE", true
        );

        UUID admin2Id = UUID.randomUUID();
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO identity.users (id, organization_id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                admin2Id, orgId, "admin2@acme.test", "hash3", "Admin", "Two", "ORG_ADMIN", true
            )
        );

        assertTrue(ex.getMessage().contains("uk_users_one_org_admin"),
            "Expected failure on uk_users_one_org_admin partial index, but got: " + ex.getMessage());
    }

    @Test
    void forceRlsShouldHideOtherOrganizationsProducts() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        setOrg(orgA);
        jdbcTemplate.update(
            "INSERT INTO identity.organizations (id, name, slug, active, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            orgA, "Org A", "org-a-" + orgA, true
        );
        setOrg(orgB);
        jdbcTemplate.update(
            "INSERT INTO identity.organizations (id, name, slug, active, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            orgB, "Org B", "org-b-" + orgB, true
        );
        jdbcTemplate.update(
            """
            INSERT INTO catalog.products (
                id, organization_id, sku, name, category, unit_price, cost_price,
                stock_quantity, reorder_level, active, version, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, 1, 1, 10, 2, true, 0, NOW(), NOW())
            """,
            productB, orgB, "SKU-B", "Hidden", "test"
        );

        setOrg(orgA);
        Integer visible = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM catalog.products WHERE id = ?",
            Integer.class,
            productB
        );
        assertEquals(0, visible, "Org A must not see Org B's product under FORCE RLS");
    }

    private void setOrg(UUID organizationId) {
        TenantContext.setOrganizationId(organizationId);
    }
}
