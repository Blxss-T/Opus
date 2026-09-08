package com.opsflow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OpsFlow API Documentation")
                .version("1.0.0")
                .description("REST API specifications for OpsFlow Business Operations Management Platform")
                .contact(new Contact()
                    .name("OpsFlow Engineering Team")
                    .email("support@opsflow.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://opsflow.com")));
    }
}
