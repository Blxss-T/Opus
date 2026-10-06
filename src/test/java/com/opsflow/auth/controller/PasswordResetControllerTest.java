package com.opsflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.otp.service.OtpCodeGenerator;
import com.opsflow.testsupport.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @MockBean
    private OtpCodeGenerator otpCodeGenerator;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
        when(otpCodeGenerator.generate()).thenReturn("654321");
    }

    private void registerAdmin(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
            "Stark Industries",
            email,
            "jarvisPassword123",
            "Tony",
            "Stark"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldResetPasswordWithOtpCode() throws Exception {
        registerAdmin("reset1@stark.com");

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset1@stark.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset1@stark.com\",\"code\":\"654321\",\"newPassword\":\"brandNewPassword789\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Old password no longer works.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset1@stark.com\",\"password\":\"jarvisPassword123\"}"))
            .andExpect(status().isUnauthorized());

        // New password works.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset1@stark.com\",\"password\":\"brandNewPassword789\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldNotRevealWhetherAccountExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ghost@nowhere.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value(
                "If an account exists for this email, a password reset code has been sent."));
    }

    @Test
    void shouldRejectResetWithWrongCode() throws Exception {
        registerAdmin("reset2@stark.com");

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset2@stark.com\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset2@stark.com\",\"code\":\"000000\",\"newPassword\":\"brandNewPassword789\"}"))
            .andExpect(status().isBadRequest());

        // Old password still works after failed reset.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset2@stark.com\",\"password\":\"jarvisPassword123\"}"))
            .andExpect(status().isOk());
    }
}
