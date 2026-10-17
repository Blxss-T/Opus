package com.opsflow.audit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    private static final String PRODUCT_JSON = "{\"sku\":\"A-001\",\"name\":\"Widget\",\"category\":\"Widgets\","
        + "\"unitPrice\":10.00,\"costPrice\":6.00,\"initialStock\":5,\"reorderLevel\":2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private String adminAToken;
    private String managerAToken;
    private String adminBToken;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
        Organization orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));
        adminAToken = jwtService.generateToken(
            userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN)));
        managerAToken = jwtService.generateToken(
            userRepository.save(new User(orgA, "manager@alpha.com", "hash", "Mana", "Alpha", Role.MANAGER)));
        adminBToken = jwtService.generateToken(
            userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN)));
    }

    @Test
    void successfulChangesShouldBeRecordedWithActorAndResource() throws Exception {
        String productId = createProduct("Bearer " + managerAToken, null);

        mockMvc.perform(put("/api/v1/products/" + productId)
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Widget v2\",\"category\":\"Widgets\",\"unitPrice\":11.00,\"costPrice\":6.00,\"reorderLevel\":2}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(2)))
            // newest first
            .andExpect(jsonPath("$.data.content[0].method").value("PUT"))
            .andExpect(jsonPath("$.data.content[0].actor").value("admin@alpha.com"))
            .andExpect(jsonPath("$.data.content[0].actorType").value("USER"))
            .andExpect(jsonPath("$.data.content[0].resourceType").value("products"))
            .andExpect(jsonPath("$.data.content[0].resourceId").value(productId))
            .andExpect(jsonPath("$.data.content[0].statusCode").value(200))
            .andExpect(jsonPath("$.data.content[1].method").value("POST"))
            .andExpect(jsonPath("$.data.content[1].actor").value("manager@alpha.com"))
            .andExpect(jsonPath("$.data.content[1].path").value("/api/v1/products"))
            .andExpect(jsonPath("$.data.content[1].statusCode").value(201));
    }

    @Test
    void readsAndFailedRequestsShouldNotBeRecorded() throws Exception {
        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void changesMadeWithAnApiKeyShouldBeAttributedToTheKey() throws Exception {
        String response = mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Warehouse sync\",\"scopes\":[\"products:write\"]}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String apiKey = objectMapper.readTree(response).at("/data/key").asText();

        createProduct(null, apiKey);

        mockMvc.perform(get("/api/v1/audit-log?actorType=API_KEY").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].actor").value("api-key:Warehouse sync"))
            .andExpect(jsonPath("$.data.content[0].resourceType").value("products"));

        // Creating the key itself was a change made by the admin.
        mockMvc.perform(get("/api/v1/audit-log?resourceType=api-keys").header("Authorization", "Bearer " + adminAToken))
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].actor").value("admin@alpha.com"));
    }

    @Test
    void auditLogShouldBeOrgAdminOnlyAndTenantIsolated() throws Exception {
        createProduct("Bearer " + adminAToken, null);

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/audit-log?actor=ADMIN@alpha").header("Authorization", "Bearer " + adminAToken))
            .andExpect(jsonPath("$.data.content", hasSize(1)));
        mockMvc.perform(get("/api/v1/audit-log?actor=nobody").header("Authorization", "Bearer " + adminAToken))
            .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    private String createProduct(String bearer, String apiKey) throws Exception {
        var request = post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON);
        request = bearer != null ? request.header("Authorization", bearer) : request.header("X-API-Key", apiKey);
        String response = mockMvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asText();
    }
}
