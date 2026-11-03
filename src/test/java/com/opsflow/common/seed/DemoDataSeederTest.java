package com.opsflow.common.seed;

import com.opsflow.testsupport.TestDatabaseCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
class DemoDataSeederTest {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        seeder.run(null);
    }

    @AfterEach
    void tearDown() {
        // Other test classes share this in-memory database.
        databaseCleaner.clean();
    }

    @Test
    void shouldSeedDataForEveryModule() {
        assertEquals(1, count("organizations"));
        assertEquals(3, count("users"));
        assertEquals(3, count("employees"));
        assertEquals(3, count("customers"));
        assertEquals(2, count("suppliers"));
        assertEquals(6, count("products"));
        assertEquals(3, count("purchase_orders"));
        assertEquals(5, count("sales_orders"));
        assertEquals(2, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sales_orders WHERE status = 'FULFILLED'", Integer.class));
        assertTrue(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM products WHERE stock_quantity <= reorder_level", Integer.class) >= 1,
            "Expected at least one low-stock product");
        // Received PO added 50 and the fulfilled SO removed 20 from the initial 120.
        assertEquals(150, jdbcTemplate.queryForObject(
            "SELECT stock_quantity FROM products WHERE sku = 'COF-001'", Integer.class));
    }

    @Test
    void shouldBeIdempotent() {
        seeder.run(null);

        assertEquals(1, count("organizations"));
        assertEquals(6, count("products"));
        assertEquals(5, count("sales_orders"));
    }

    @Test
    void seededUsersShouldBeAbleToSignIn() throws Exception {
        for (String email : new String[] {
            DemoDataSeeder.ADMIN_EMAIL, DemoDataSeeder.MANAGER_EMAIL, DemoDataSeeder.EMPLOYEE_EMAIL
        }) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"DemoPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        }
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
