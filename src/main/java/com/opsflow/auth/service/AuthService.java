package com.opsflow.auth.service;

import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.dto.OrganizationResponse;
import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.security.JwtService;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.service.OrganizationProvisioningService;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final OrganizationProvisioningService organizationProvisioningService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        OrganizationProvisioningService organizationProvisioningService,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.organizationProvisioningService = organizationProvisioningService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "User with this email already exists");
        }

        Organization organization = organizationProvisioningService.createOrganization(request.organizationName());
        validateSingleOrgAdminConstraint(organization.getId(), Role.ORG_ADMIN);

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(
            organization,
            request.email().toLowerCase(Locale.ROOT).trim(),
            encodedPassword,
            request.firstName().trim(),
            request.lastName().trim(),
            Role.ORG_ADMIN
        );
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.of(
            token,
            UserResponse.fromEntity(user),
            OrganizationResponse.fromEntity(organization)
        );
    }

    @Transactional
    public User createUserInOrganization(
        Organization organization,
        String email,
        String rawPassword,
        String firstName,
        String lastName,
        Role role
    ) {
        Role targetRole = role != null ? role : Role.EMPLOYEE;
        validateSingleOrgAdminConstraint(organization.getId(), targetRole);

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(
            organization,
            email.toLowerCase(Locale.ROOT).trim(),
            encodedPassword,
            firstName.trim(),
            lastName.trim(),
            targetRole
        );
        return userRepository.save(user);
    }

    public void validateSingleOrgAdminConstraint(UUID organizationId, Role role) {
        if (role == Role.ORG_ADMIN && userRepository.existsByOrganizationIdAndRole(organizationId, Role.ORG_ADMIN)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "An organization can only have one ORG_ADMIN during the pilot phase"
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT).trim();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isActive() || !user.getOrganization().isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Account or organization is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.of(
            token,
            UserResponse.fromEntity(user),
            OrganizationResponse.fromEntity(user.getOrganization())
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthenticated request");
        }
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        return UserResponse.fromEntity(user);
    }
}
