package com.opsflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.testsupport.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "security.step-up.enabled=true")
class StepUpAuthControllerTest {

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

    @Test
    void organizationRenameShouldRequireRecentPasswordConfirmation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(
                    "Step Up Corp", "admin@stepup.test", "Password123!", "Ada", "Admin"))))
            .andExpect(status().isCreated());

        String token = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin@stepup.test", "Password123!"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString())
            .at("/data/accessToken").asText();

        mockMvc.perform(put("/api/v1/organization")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed Corp\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.message").value("Re-authentication required"));

        String steppedUp = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/step-up")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn().getResponse().getContentAsString())
            .at("/data/accessToken").asText();

        mockMvc.perform(put("/api/v1/organization")
                .header("Authorization", "Bearer " + steppedUp)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed Corp\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Renamed Corp"));
    }
}
