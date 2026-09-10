package com.opsflow.auth.security;

import com.opsflow.organizations.domain.Organization;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        long expirationMs = 3600000; // 1 hour
        jwtService = new JwtService(secret, expirationMs);

        testOrg = new Organization("Acme Corp", "acme-corp");
        testOrg.setId(UUID.randomUUID());

        testUser = new User(testOrg, "john@acme.com", "hashed_pass", "John", "Doe", Role.ORG_ADMIN);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("john@acme.com", jwtService.getEmailFromToken(token));
        assertEquals(testUser.getId(), jwtService.getUserIdFromToken(token));
        assertEquals(testOrg.getId(), jwtService.getOrganizationIdFromToken(token));
        assertEquals(Role.ORG_ADMIN, jwtService.getRoleFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
    }
}
