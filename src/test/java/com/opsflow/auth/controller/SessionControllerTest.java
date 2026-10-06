package com.opsflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.testsupport.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
    }

    private record Tokens(String accessToken, String refreshToken) {}

    private Tokens registerAdmin(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
            "Stark Industries",
            email,
            "jarvisPassword123",
            "Tony",
            "Stark"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        return new Tokens(
            objectMapper.readTree(json).path("data").path("accessToken").asText(),
            objectMapper.readTree(json).path("data").path("refreshToken").asText()
        );
    }

    @Test
    void shouldRefreshSessionAndRotateToken() throws Exception {
        Tokens tokens = registerAdmin("refresh1@stark.com");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", tokens.refreshToken()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andReturn();

        String json = refreshResult.getResponse().getContentAsString();
        String newRefreshToken = objectMapper.readTree(json).path("data").path("refreshToken").asText();
        String newAccessToken = objectMapper.readTree(json).path("data").path("accessToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(tokens.refreshToken());

        // The rotated old token must be rejected (single use).
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", tokens.refreshToken()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", "bogus-token"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutAndInvalidateRefreshTokens() throws Exception {
        Tokens tokens = registerAdmin("logout1@stark.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", tokens.refreshToken()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldChangePasswordAndRevokeSessions() throws Exception {
        Tokens tokens = registerAdmin("changepw1@stark.com");

        mockMvc.perform(post("/api/v1/auth/password/change")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "currentPassword", "jarvisPassword123",
                    "newPassword", "newSecurePassword456"))))
            .andExpect(status().isOk());

        // Old refresh token is revoked after password change.
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("refreshToken", tokens.refreshToken()))))
            .andExpect(status().isUnauthorized());

        // Login with the new password works.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new com.opsflow.auth.dto.LoginRequest("changepw1@stark.com", "newSecurePassword456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void shouldRejectAccessTokenIssuedBeforePasswordChange() throws Exception {
        Tokens tokens = registerAdmin("tokeninvalidate@stark.com");

        mockMvc.perform(post("/api/v1/auth/password/change")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "currentPassword", "jarvisPassword123",
                    "newPassword", "newSecurePassword456"))))
            .andExpect(status().isOk());

        // The pre-change access token must be rejected by the JWT filter.
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isUnauthorized());
    }
}
