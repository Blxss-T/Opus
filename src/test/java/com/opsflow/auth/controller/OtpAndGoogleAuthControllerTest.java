package com.opsflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.otp.service.OtpCodeGenerator;
import com.opsflow.employees.repository.EmployeeRepository;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.repository.UserRepository;
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
class OtpAndGoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @MockBean
    private OtpCodeGenerator otpCodeGenerator;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        when(otpCodeGenerator.generate()).thenReturn("123456");
    }

    @Test
    void shouldSendAndVerifyOtpWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"verify@opus.test","type":"EMAIL_VERIFICATION"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.success").value(true));

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"verify@opus.test","code":"123456","type":"EMAIL_VERIFICATION"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRejectReusedOtp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"reuse@opus.test","type":"EMAIL_VERIFICATION"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"reuse@opus.test","code":"123456","type":"EMAIL_VERIFICATION"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"reuse@opus.test","code":"123456","type":"EMAIL_VERIFICATION"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAuthenticateWithMockGoogleTokenInTestProfile() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("idToken", "mock-google-token:ada@opus.test:Ada:Lovelace"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.email").value("ada@opus.test"))
            .andExpect(jsonPath("$.data.user.role").value("ORG_ADMIN"))
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void shouldRejectInvalidGoogleToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("idToken", "not-a-google-token"))))
            .andExpect(status().isUnauthorized());
    }
}
