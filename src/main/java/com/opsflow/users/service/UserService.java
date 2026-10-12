package com.opsflow.users.service;

import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.auth.service.AuthService;
import com.opsflow.auth.session.service.RefreshTokenService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.common.jpa.FilterSpecs;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.dto.CreateUserRequest;
import com.opsflow.users.dto.UpdateUserRequest;
import com.opsflow.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Org-scoped user administration. The ORG_ADMIN creates MANAGER and EMPLOYEE
 * accounts with an initial password; the admin account itself is managed
 * only through registration and the password endpoints.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public UserService(
        UserRepository userRepository,
        OrganizationRepository organizationRepository,
        AuthService authService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse createUser(UserPrincipal principal, CreateUserRequest request) {
        requireAssignableRole(request.role());

        UUID organizationId = principal.getOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        String email = request.email().toLowerCase(Locale.ROOT).trim();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "User with this email already exists");
        }

        User user = authService.createUserInOrganization(
            organization,
            email,
            request.password(),
            request.firstName(),
            request.lastName(),
            request.role()
        );
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(UserPrincipal principal, String q, Role role, Boolean active, Pageable pageable) {
        Specification<User> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.ilikeAny(q, "firstName", "lastName", "email"),
            FilterSpecs.equalsValue("role", role),
            FilterSpecs.equalsValue("active", active)
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.asc("firstName"), Sort.Order.asc("lastName"));
        return userRepository.findAll(spec, effective).map(UserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UserPrincipal principal, UUID userId) {
        return UserResponse.fromEntity(findUserForTenant(principal.getOrganizationId(), userId));
    }

    @Transactional
    public UserResponse updateUser(UserPrincipal principal, UUID userId, UpdateUserRequest request) {
        User user = findUserForTenant(principal.getOrganizationId(), userId);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.role() != null && request.role() != user.getRole()) {
            requireNotOrgAdmin(user, "The ORG_ADMIN role cannot be changed");
            requireAssignableRole(request.role());
            user.setRole(request.role());
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(UserPrincipal principal, UUID userId) {
        User user = findUserForTenant(principal.getOrganizationId(), userId);
        requireNotOrgAdmin(user, "The ORG_ADMIN account cannot be deactivated");

        user.setActive(false);
        User saved = userRepository.save(user);
        refreshTokenService.revokeAllForUser(saved.getId());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse activateUser(UserPrincipal principal, UUID userId) {
        User user = findUserForTenant(principal.getOrganizationId(), userId);
        user.setActive(true);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    private void requireAssignableRole(Role role) {
        if (role != Role.MANAGER && role != Role.EMPLOYEE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Role must be MANAGER or EMPLOYEE");
        }
    }

    private void requireNotOrgAdmin(User user, String message) {
        if (user.getRole() == Role.ORG_ADMIN) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
        }
    }

    private User findUserForTenant(UUID organizationId, UUID userId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
