package com.opsflow.auth.service;

import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.security.JwtService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;

    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtService = new JwtService(secret, 3600000);
        authService = new AuthService(organizationRepository, userRepository, passwordEncoder, jwtService);

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
        when(organizationRepository.existsBySlug(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        Organization mockOrg = new Organization("Acme Corp", "acme-corp");
        mockOrg.setId(UUID.randomUUID());
        when(organizationRepository.save(any(Organization.class))).thenReturn(mockOrg);

        User mockUser = new User(mockOrg, "admin@acme.com", "hashedPassword", "Jane", "Doe", Role.ORG_ADMIN);
        mockUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("admin@acme.com", response.user().email());
        assertEquals("Acme Corp", response.organization().name());

        verify(organizationRepository).save(any(Organization.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailRegistrationIfEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(registerRequest));
        verify(organizationRepository, never()).save(any());
        verify(userRepository, never()).save(any());
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
