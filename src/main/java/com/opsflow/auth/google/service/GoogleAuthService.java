package com.opsflow.auth.google.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.OrganizationResponse;
import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.security.JwtService;
import com.opsflow.auth.service.AuthService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.service.OrganizationProvisioningService;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final OrganizationProvisioningService organizationProvisioningService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthService authService;
    private final GoogleIdTokenVerifier verifier;
    private final boolean allowMockTokens;

    public GoogleAuthService(
        UserRepository userRepository,
        OrganizationProvisioningService organizationProvisioningService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthService authService,
        @Value("${security.google.client-id:dummy-client-id}") String googleClientId,
        @Value("${security.google.allow-mock-tokens:false}") boolean allowMockTokens
    ) {
        this.userRepository = userRepository;
        this.organizationProvisioningService = organizationProvisioningService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authService = authService;
        this.allowMockTokens = allowMockTokens;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build();
    }

    @Transactional
    public AuthResponse authenticateGoogleUser(String idTokenString) {
        GoogleUserInfo googleUser = verifyAndExtractGoogleInfo(idTokenString);

        String email = googleUser.email().toLowerCase(Locale.ROOT).trim();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        User user;
        Organization organization;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            organization = user.getOrganization();
            if (!user.isActive() || !organization.isActive()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Account or organization is inactive");
            }
        } else {
            String orgName = (googleUser.firstName() != null ? googleUser.firstName() : "User") + "'s Organization";
            organization = organizationProvisioningService.createOrganization(orgName);
            authService.validateSingleOrgAdminConstraint(organization.getId(), Role.ORG_ADMIN);

            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            user = new User(
                organization,
                email,
                randomPassword,
                googleUser.firstName() != null ? googleUser.firstName() : "Google",
                googleUser.lastName() != null ? googleUser.lastName() : "User",
                Role.ORG_ADMIN
            );
            user.setEmailVerifiedAt(Instant.now());
            user = userRepository.save(user);
        }

        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.of(
            jwtToken,
            UserResponse.fromEntity(user),
            OrganizationResponse.fromEntity(organization)
        );
    }

    private GoogleUserInfo verifyAndExtractGoogleInfo(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                return new GoogleUserInfo(email, firstName, lastName);
            }
        } catch (Exception ex) {
            log.warn("Google ID token verification failed: {}", ex.getMessage());
        }

        if (allowMockTokens && idTokenString != null && idTokenString.startsWith("mock-google-token:")) {
            String[] parts = idTokenString.split(":");
            String email = parts.length > 1 ? parts[1] : "googleuser@gmail.com";
            String firstName = parts.length > 2 ? parts[2] : "Google";
            String lastName = parts.length > 3 ? parts[3] : "User";
            return new GoogleUserInfo(email, firstName, lastName);
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Google ID token");
    }

    public record GoogleUserInfo(String email, String firstName, String lastName) {}
}
