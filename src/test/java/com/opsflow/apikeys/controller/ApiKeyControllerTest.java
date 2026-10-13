package com.opsflow.apikeys.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.apikeys.domain.ApiKey;
import com.opsflow.apikeys.repository.ApiKeyRepository;
import com.opsflow.auth.security.JwtService;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.testsupport.TestDatabaseCleaner;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private String adminAToken;
    private String managerAToken;
    private String adminBToken;
    private Product productB;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();

        Organization orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        User adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        User managerA = userRepository.save(new User(orgA, "manager@alpha.com", "hash", "Mana", "Alpha", Role.MANAGER));
        User adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        managerAToken = jwtService.generateToken(managerA);
        adminBToken = jwtService.generateToken(adminB);

        productRepository.save(new Product(orgA, "A-001", "Alpha Widget", null, "Widgets",
            new BigDecimal("10.00"), new BigDecimal("6.00"), 50, 10));
        productB = productRepository.save(new Product(orgB, "B-001", "Beta Widget", null, "Widgets",
            new BigDecimal("10.00"), new BigDecimal("6.00"), 50, 10));
    }

    @Test
    void orgAdminShouldCreateKeyAndTheRawKeyIsNeverStoredOrListed() throws Exception {
        JsonNode created = createKey(adminAToken, "Storefront", "\"products:read\",\"reports:read\"");
        String rawKey = created.at("/data/key").asText();

        assertEquals("Storefront", created.at("/data/apiKey/name").asText());
        assertEquals(rawKey.substring(0, 12), created.at("/data/apiKey/keyPrefix").asText());
        assertEquals(true, created.at("/data/apiKey/active").asBoolean());

        ApiKey stored = apiKeyRepository.findAll().get(0);
        assertNotNull(stored.getKeyHash());
        assertFalse(stored.getKeyHash().contains(rawKey));
        assertEquals(64, stored.getKeyHash().length());

        mockMvc.perform(get("/api/v1/api-keys").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].keyPrefix", startsWith("opk_")))
            .andExpect(jsonPath("$.data[0].scopes[0]").value("products:read"))
            .andExpect(jsonPath("$.data[0].key").doesNotExist())
            .andExpect(jsonPath("$.data[0].keyHash").doesNotExist());
    }

    @Test
    void onlyOrgAdminShouldManageKeys() throws Exception {
        mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nope\",\"scopes\":[\"products:read\"]}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/api-keys").header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/api-keys"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidCreateRequests() throws Exception {
        String[] bodies = {
            "{\"name\":\"\",\"scopes\":[\"products:read\"]}",
            "{\"name\":\"No scopes\",\"scopes\":[]}",
            "{\"name\":\"Bad scope\",\"scopes\":[\"users:write\"]}",
            "{\"name\":\"Past expiry\",\"scopes\":[\"products:read\"],\"expiresAt\":\"2020-01-01T00:00:00Z\"}"
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/v1/api-keys")
                    .header("Authorization", "Bearer " + adminAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.error").value("ERR_400"));
        }
    }

    @Test
    void keyShouldBeLimitedToItsScopes() throws Exception {
        String key = createKey(adminAToken, "Read only", "\"products:read\"").at("/data/key").asText();

        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, key))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].sku").value("A-001"));

        mockMvc.perform(post("/api/v1/products")
                .header(API_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("A-002")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("API key is missing the required scope 'products:write'"));

        mockMvc.perform(get("/api/v1/customers").header(API_KEY_HEADER, key))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("API key is missing the required scope 'customers:read'"));
    }

    @Test
    void writeScopeShouldIncludeReadAndAttributeChangesToTheKey() throws Exception {
        String key = createKey(adminAToken, "Warehouse sync", "\"products:write\"").at("/data/key").asText();

        mockMvc.perform(post("/api/v1/products")
                .header(API_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("A-002")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.sku").value("A-002"));

        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, key))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(2)));

        assertEquals("api-key:Warehouse sync", jdbcTemplate.queryForObject(
            "SELECT performed_by_email FROM inventory_movements", String.class));
    }

    @Test
    void keyShouldNotReachUserAuthOrKeyManagementEndpoints() throws Exception {
        String key = createKey(adminAToken, "Everything", allScopes()).at("/data/key").asText();

        String[] paths = {"/api/v1/users", "/api/v1/api-keys", "/api/v1/auth/me", "/api/v1/employees/me"};
        for (String path : paths) {
            mockMvc.perform(get(path).header(API_KEY_HEADER, key))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("This endpoint is not available to API keys"));
        }

        mockMvc.perform(post("/api/v1/api-keys")
                .header(API_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Self-made\",\"scopes\":[\"products:read\"]}"))
            .andExpect(status().isForbidden());

        // Creating employees needs ORG_ADMIN and has no write scope.
        mockMvc.perform(post("/api/v1/employees")
                .header(API_KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"a@b.com\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/employees").header(API_KEY_HEADER, key))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/dashboard").header(API_KEY_HEADER, key))
            .andExpect(status().isOk());
    }

    @Test
    void keyShouldOnlySeeItsOwnOrganization() throws Exception {
        String keyA = createKey(adminAToken, "Alpha key", "\"products:read\"").at("/data/key").asText();

        mockMvc.perform(get("/api/v1/products/" + productB.getId()).header(API_KEY_HEADER, keyA))
            .andExpect(status().isNotFound());

        // Another organization's admin cannot see or revoke the key.
        UUID keyId = apiKeyRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/v1/api-keys").header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(delete("/api/v1/api-keys/" + keyId).header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidRevokedAndExpiredKeysShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, "opk_not-a-real-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.error").value("ERR_401"));

        JsonNode revoked = createKey(adminAToken, "To revoke", "\"products:read\"");
        mockMvc.perform(delete("/api/v1/api-keys/" + revoked.at("/data/apiKey/id").asText())
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.active").value(false))
            .andExpect(jsonPath("$.data.revokedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, revoked.at("/data/key").asText()))
            .andExpect(status().isUnauthorized());

        JsonNode expiring = createKey(adminAToken, "To expire", "\"products:read\"");
        ApiKey key = apiKeyRepository.findById(UUID.fromString(expiring.at("/data/apiKey/id").asText())).orElseThrow();
        key.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        apiKeyRepository.save(key);
        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, expiring.at("/data/key").asText()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void usingAKeyShouldRecordWhenItWasLastUsed() throws Exception {
        JsonNode created = createKey(adminAToken, "Tracked", "\"products:read\"");

        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, created.at("/data/key").asText()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/api-keys").header("Authorization", "Bearer " + adminAToken))
            .andExpect(jsonPath("$.data[0].lastUsedAt").isNotEmpty());
    }

    private JsonNode createKey(String adminToken, String name, String scopesJson) throws Exception {
        String response = mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"scopes\":[" + scopesJson + "]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.key", startsWith("opk_")))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String allScopes() throws Exception {
        String response = mockMvc.perform(get("/api/v1/api-keys/scopes").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String array = objectMapper.readTree(response).at("/data").toString();
        return array.substring(1, array.length() - 1);
    }

    private String productJson(String sku) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"Widget " + sku + "\",\"category\":\"Widgets\","
            + "\"unitPrice\":10.00,\"costPrice\":6.00,\"initialStock\":5,\"reorderLevel\":2}";
    }
}
