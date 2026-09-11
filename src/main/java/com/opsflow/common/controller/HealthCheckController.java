package com.opsflow.common.controller;

import com.opsflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Endpoints for service health & status verification")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "Get API Health Status", description = "Returns operational status and system details of the Opus API service.")
    public ApiResponse<Map<String, Object>> getHealthStatus() {
        Map<String, Object> statusInfo = Map.of(
            "status", "UP",
            "service", "Opus API Core",
            "environment", "active"
        );
        return ApiResponse.success(statusInfo);
    }
}
