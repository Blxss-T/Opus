package com.opsflow.common.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the application on a real PostgreSQL database built from the Flyway
 * migrations, seeds the demo data, and reads every list and report endpoint.
 * Covers what the H2-based tests cannot: the migrations, schema validation
 * and the reporting queries running on the production database engine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles({"it", "seed"})
@Tag("integration")
@EnabledIf("isDockerAvailable")
class DemoDataSmokeIT {

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
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededDatabaseShouldServeEveryListAndReportEndpoint() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + DemoDataSeeder.ADMIN_EMAIL + "\",\"password\":\"DemoPass123!\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        String bearer = "Bearer " + body.at("/data/accessToken").asText();

        for (String path : new String[] {
            "/api/v1/users", "/api/v1/employees", "/api/v1/customers", "/api/v1/suppliers",
            "/api/v1/products", "/api/v1/purchase-orders", "/api/v1/sales-orders"
        }) {
            mockMvc.perform(get(path).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty());
        }

        for (String path : new String[] {
            "/api/v1/reports/dashboard/low-stock", "/api/v1/reports/sales/by-category",
            "/api/v1/reports/purchases/by-category", "/api/v1/reports/sales/top-customers",
            "/api/v1/reports/purchases/top-suppliers", "/api/v1/reports/sales/daily",
            "/api/v1/reports/sales/monthly-trend"
        }) {
            mockMvc.perform(get(path).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty());
        }

        mockMvc.perform(get("/api/v1/reports/dashboard").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").exists());
    }
}
