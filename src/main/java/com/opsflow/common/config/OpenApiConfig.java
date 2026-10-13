package com.opsflow.common.config;

import com.opsflow.common.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    static final String ERROR_SCHEMA = "ApiError";

    /** Endpoints that need no token; keep in sync with SecurityConfig. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/v1/health",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/google",
        "/api/v1/auth/otp/send",
        "/api/v1/auth/otp/verify",
        "/api/v1/auth/refresh",
        "/api/v1/auth/password/forgot",
        "/api/v1/auth/password/reset"
    );

    /** Public endpoints behind the IP rate limiter; keep in sync with RateLimitFilter. */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/google",
        "/api/v1/auth/otp/send",
        "/api/v1/auth/password/forgot",
        "/api/v1/auth/password/reset"
    );

    private static final String DESCRIPTION = """
        REST API for the Opus Business Operations Management Platform.

        **Authentication.** Call `POST /api/v1/auth/login` (or `/auth/register`), copy `data.accessToken`, \
        click **Authorize** and paste it. Access tokens are short-lived; exchange `data.refreshToken` at \
        `POST /api/v1/auth/refresh` for a new pair.

        **Other applications.** An `ORG_ADMIN` creates an API key at `POST /api/v1/api-keys` and the         application sends it as the `X-API-Key` header instead of a token. A key is limited to its scopes         (`products:read`, `sales-orders:write`, ...; `:write` includes `:read`) and cannot reach the         user, auth or API key endpoints.

        **Responses.** Every response is wrapped in an envelope: `success`, `timestamp`, and either `data` \
        or `error`. Errors carry a stable `error.error` code (`ERR_400`, `ERR_401`, `ERR_403`, `ERR_404`, \
        `ERR_409`, `ERR_422`, `ERR_503`, `ERR_500`) and, for validation failures, per-field `error.details`.

        **Lists.** List endpoints accept `page` (0-based), `size` (default 20, max 100) and `sort` \
        (e.g. `name,asc`), and return `data.content[]` with `data.page` metadata.
        """;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
                .addSecuritySchemes("api-key", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("For other applications. Created by the ORG_ADMIN at POST /api/v1/api-keys.")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .addSecurityItem(new SecurityRequirement().addList("api-key"))
            .info(new Info()
                .title("Opus API Documentation")
                .version("1.0.0")
                .description(DESCRIPTION)
                .contact(new Contact()
                    .name("Opus Engineering Team")
                    .email("support@opus.local"))
                .license(new License()
                    .name("Proprietary")));
    }

    /**
     * Documents the error responses every operation can return, so they do
     * not have to be repeated on each controller method, and removes the
     * token requirement from public endpoints.
     */
    @Bean
    public OpenApiCustomizer standardErrorResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            // Registered here rather than on the OpenAPI bean: springdoc drops
            // schemas that nothing references before customizers run.
            openApi.getComponents().addSchemas(ERROR_SCHEMA, errorEnvelopeSchema());
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                boolean isPublic = PUBLIC_PATHS.contains(path);
                if (isPublic) {
                    operation.setSecurity(List.of());
                }
                errorResponsesFor(path, method.name(), isPublic).forEach((status, errorCode) -> addErrorResponse(operation, status, errorCode));
                if (RATE_LIMITED_PATHS.contains(path)) {
                    addResponse(operation, "429", "Too many requests from this IP; retry after the `Retry-After` header (seconds)", null);
                }
            }));
        };
    }

    private Map<String, ErrorCode> errorResponsesFor(String path, String method, boolean isPublic) {
        boolean hasBody = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        Map<String, ErrorCode> responses = new LinkedHashMap<>();
        responses.put("400", ErrorCode.VALIDATION_ERROR);
        if (!path.equals("/api/v1/health")) {
            responses.put("401", ErrorCode.UNAUTHORIZED);
        }
        if (!isPublic) {
            responses.put("403", ErrorCode.FORBIDDEN);
        }
        if (path.contains("{")) {
            responses.put("404", ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (hasBody && !isPublic) {
            responses.put("409", ErrorCode.CONFLICT);
        }
        if (hasBody) {
            responses.put("422", ErrorCode.BUSINESS_RULE_VIOLATION);
        }
        responses.put("500", ErrorCode.INTERNAL_SERVER_ERROR);
        return responses;
    }

    private void addErrorResponse(Operation operation, String status, ErrorCode errorCode) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", errorCode.getHttpStatus().value());
        error.put("error", errorCode.getCode());
        error.put("message", errorCode.getDefaultMessage());
        error.put("path", "/api/v1/...");
        error.put("timestamp", "2026-01-01T12:00:00Z");

        Map<String, Object> example = new LinkedHashMap<>();
        example.put("success", false);
        example.put("error", error);
        example.put("timestamp", "2026-01-01T12:00:00Z");

        addResponse(operation, status, errorCode.getDefaultMessage(), example);
    }

    private void addResponse(Operation operation, String status, String description, Object example) {
        if (operation.getResponses().containsKey(status)) {
            return;
        }
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA));
        if (example != null) {
            mediaType.setExample(example);
        }
        operation.getResponses().addApiResponse(status, new ApiResponse()
            .description(description)
            .content(new Content().addMediaType("application/json", mediaType)));
    }

    private Schema<?> errorEnvelopeSchema() {
        Schema<?> fieldError = new ObjectSchema()
            .addProperty("field", new StringSchema())
            .addProperty("message", new StringSchema())
            .addProperty("rejectedValue", new Schema<>().description("The value that failed validation"));

        Schema<?> error = new ObjectSchema()
            .addProperty("status", new IntegerSchema().description("HTTP status code"))
            .addProperty("error", new StringSchema().description("Stable machine-readable code, e.g. ERR_422"))
            .addProperty("message", new StringSchema().description("Human-readable explanation"))
            .addProperty("path", new StringSchema())
            .addProperty("details", new ArraySchema().items(fieldError).description("Per-field errors; present on validation failures"))
            .addProperty("timestamp", new DateTimeSchema());

        return new ObjectSchema()
            .description("Error envelope returned by every failed request")
            .addProperty("success", new BooleanSchema().example(false))
            .addProperty("error", error)
            .addProperty("timestamp", new DateTimeSchema());
    }
}
