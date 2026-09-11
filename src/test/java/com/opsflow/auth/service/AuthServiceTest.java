package com.opsflow.auth.service;

import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.security.JwtService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.service.OrganizationProvisioningService;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OrganizationProvisioningService organizationProvisioningService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        JwtService jwtService = new JwtService(secret, 3600000);
        authService = new AuthService(organizationProvisioningService, userRepository, passwordEncoder, jwtService);

        registerRequest = new RegisterRequest(
            "Acme Corp",
            "admin@acme.com",
            "securePassword123",
            "Jane",
            "Doe"
        );

        loginRequest = new LoginRequest("admin@acme.com", "securePassword123");
    }

    @Test
    void shouldRegisterNewOrganizationAndUserSuccessfully() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        Organization mockOrg = new Organization("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());
        when(organizationProvisioningService.createOrganization("Acme Corp")).thenReturn(mockOrg);
        when(userRepository.existsByOrganizationIdAndRole(mockOrg.getId(), Role.ORG_ADMIN)).thenReturn(false);

        User mockUser = new User(mockOrg, "admin@acme.com", "hashedPassword", "Jane", "Doe", Role.ORG_ADMIN);
        mockUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("admin@acme.com", response.user().email());
        assertEquals("Acme Corp", response.organization().name());

        verify(organizationProvisioningService).createOrganization("Acme Corp");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailRegistrationIfEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(organizationProvisioningService, never()).createOrganization(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectSecondOrgAdmin() {
        UUID orgId = UUID.randomUUID();
        when(userRepository.existsByOrganizationIdAndRole(orgId, Role.ORG_ADMIN)).thenReturn(true);

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> authService.validateSingleOrgAdminConstraint(orgId, Role.ORG_ADMIN)
        );
        assertEquals("ERR_422", ex.getErrorCode().getCode());
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        Organization mockOrg = new Organization("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());

        User mockUser = new User(mockOrg, "admin@acme.com", "hashedPassword", "Jane", "Doe", Role.ORG_ADMIN);
        mockUser.setId(UUID.randomUUID());

        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("securePassword123", "hashedPassword")).thenReturn(true);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("admin@acme.com", response.user().email());
    }

    @Test
    void shouldFailLoginWithInvalidPassword() {
        Organization mockOrg = new Organization("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());

        User mockUser = new User(mockOrg, "admin@acme.com", "hashedPassword", "Jane", "Doe", Role.ORG_ADMIN);
        mockUser.setId(UUID.randomUUID());

        when(userRepository.findByEmail("admin@acme.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        LoginRequest wrongLogin = new LoginRequest("admin@acme.com", "wrongPassword");

        assertThrows(BusinessException.class, () -> authService.login(wrongLogin));
    }
}
