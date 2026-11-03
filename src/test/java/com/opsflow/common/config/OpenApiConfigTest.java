package com.opsflow.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointsShouldDocumentStandardErrorsAndRequireToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists())
            .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['401']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['403']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/products'].post.responses['422']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/products'].post.security").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/v1/products/{id}'].get.responses['404'].content['application/json'].example.error.error").value("ERR_404"));
    }

    @Test
    void publicEndpointsShouldNotRequireTokenAndShouldDocumentRateLimit() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty())
            .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['429']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['403']").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post.responses['429']").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/v1/health'].get.security").isEmpty());
    }
}
