package com.opsflow.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecretsValidator {

    public ProductionSecretsValidator(@Value("${security.jwt.secret-key:}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be set to a unique value of at least 32 characters in production");
        }
        if ("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970".equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must not use the committed development default in production");
        }
    }
}
