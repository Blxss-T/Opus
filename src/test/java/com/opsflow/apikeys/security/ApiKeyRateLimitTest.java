package com.opsflow.apikeys.security;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "security.api-keys.rate-limit.limit=3")
class ApiKeyRateLimitTest {

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
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private String adminToken;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
        Organization org = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        User admin = userRepository.save(new User(org, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        adminToken = jwtService.generateToken(admin);
    }

    @Test
    void eachKeyShouldHaveItsOwnLimitAcrossEndpoints() throws Exception {
        String busyKey = createKey("Busy");
        String quietKey = createKey("Quiet");

        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, busyKey)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/customers").header(API_KEY_HEADER, busyKey)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, busyKey)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customers").header(API_KEY_HEADER, busyKey))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "60"))
            .andExpect(jsonPath("$.error.error").value("ERR_429"));

        mockMvc.perform(get("/api/v1/products").header(API_KEY_HEADER, quietKey)).andExpect(status().isOk());

        // Signed-in users are not subject to the API key limit.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        }
    }

    private String createKey(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"scopes\":[\"products:read\",\"customers:read\"]}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/key").asText();
    }
}
