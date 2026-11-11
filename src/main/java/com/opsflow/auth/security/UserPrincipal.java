package com.opsflow.auth.security;

import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID organizationId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final Instant reauthExpiresAt;
    private final boolean apiKey;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, UUID organizationId, String email, String passwordHash, Role role, boolean active) {
        this(id, organizationId, email, passwordHash, role, active, null, false);
    }

    public UserPrincipal(
        UUID id,
        UUID organizationId,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        Instant reauthExpiresAt,
        boolean apiKey
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.reauthExpiresAt = reauthExpiresAt;
        this.apiKey = apiKey;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getOrganization().getId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole(),
            user.isActive()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getReauthExpiresAt() {
        return reauthExpiresAt;
    }

    public boolean isApiKey() {
        return apiKey;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
