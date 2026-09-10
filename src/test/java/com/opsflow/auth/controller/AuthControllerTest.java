package com.opsflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void shouldRegisterLoginAndGetProfileSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
            "Stark Industries",
            "tony@stark.com",
            "jarvisPassword123",
            "Tony",
            "Stark"
        );

        // 1. Register Organization & Admin User
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value("tony@stark.com"))
            .andExpect(jsonPath("$.data.organization.name").value("Stark Industries"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn();

        // 2. Login User
        LoginRequest loginRequest = new LoginRequest("tony@stark.com", "jarvisPassword123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).path("data").path("accessToken").asText();

        // 3. Access Protected Profile /api/v1/auth/me with Bearer Token
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("tony@stark.com"))
            .andExpect(jsonPath("$.data.role").value("ORG_ADMIN"));

        // 4. Verify Unauthenticated Request returns 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailRegistrationForInvalidData() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
            "",
            "not-an-email",
            "short",
            "",
            ""
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.error").value("ERR_400"));
    }
}
