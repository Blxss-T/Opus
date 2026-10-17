package com.opsflow.auth.password.controller;

import com.opsflow.auth.password.PasswordResetService;
import com.opsflow.common.exception.GlobalExceptionHandler;
import com.opsflow.common.mail.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetControllerUnitTest {

    @Test
    void forgotPasswordShouldNotRevealAccountExistenceWhenEmailDeliveryFails() throws Exception {
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        doThrow(new EmailDeliveryException("smtp down")).when(passwordResetService).requestReset("user@example.com");
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PasswordResetController(passwordResetService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
