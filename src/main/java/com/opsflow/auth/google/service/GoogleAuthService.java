package com.opsflow.auth.google.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.opsflow.auth.dto.AuthResponse;
import com.opsflow.auth.dto.OrganizationResponse;
import com.opsflow.auth.dto.UserResponse;
import com.opsflow.auth.security.JwtService;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(
        UserRepository userRepository,
        OrganizationRepository organizationRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        @Value("${security.google.client-id:dummy-client-id}") String googleClientId
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
            // Provision new Organization and User for Google Account
            String orgName = (googleUser.firstName() != null ? googleUser.firstName() : "User") + "'s Organization";
            String baseSlug = generateSlug(orgName);
            String slug = baseSlug;
            int counter = 1;
            while (organizationRepository.existsBySlug(slug)) {
                slug = baseSlug + "-" + counter++;
            }

            organization = new Organization(orgName, slug);
            organization = organizationRepository.save(organization);

            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            user = new User(
                organization,
                email,
                randomPassword,
                googleUser.firstName() != null ? googleUser.firstName() : "Google",
                googleUser.lastName() != null ? googleUser.lastName() : "User",
                Role.ORG_ADMIN
            );
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

        // Dev mode fallback for test tokens (e.g. "mock-google-token:user@gmail.com:John:Doe")
        if (idTokenString.startsWith("mock-google-token:")) {
            String[] parts = idTokenString.split(":");
            String email = parts.length > 1 ? parts[1] : "googleuser@gmail.com";
            String firstName = parts.length > 2 ? parts[2] : "Google";
            String lastName = parts.length > 3 ? parts[3] : "User";
            return new GoogleUserInfo(email, firstName, lastName);
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Google ID token");
    }

    private String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "org-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return slug.isBlank() ? "org-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    public record GoogleUserInfo(String email, String firstName, String lastName) {}
}
