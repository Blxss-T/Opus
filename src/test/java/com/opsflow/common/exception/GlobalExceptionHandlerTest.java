package com.opsflow.common.exception;

import com.opsflow.common.mail.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new ConflictingController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    void optimisticLockFailureShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.error").value("ERR_409"))
            .andExpect(jsonPath("$.error.path").value("/test/conflict"));
    }

    @Test
    void emailDeliveryFailureShouldReturn503() throws Exception {
        mockMvc.perform(get("/test/mail-failure"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.error").value("ERR_503"));
    }

    @Test
    void malformedOrMistypedBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NOT_A_STATUS\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error").value("ERR_400"));

        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error").value("ERR_400"));
    }

    enum Status { OPEN }

    record Body(Status status) {}

    @RestController
    static class ConflictingController {

        @PostMapping("/test/body")
        String body(@RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new ObjectOptimisticLockingFailureException("Product", "stale");
        }

        @GetMapping("/test/mail-failure")
        String mailFailure() {
            throw new EmailDeliveryException("smtp down");
        }
    }
}
