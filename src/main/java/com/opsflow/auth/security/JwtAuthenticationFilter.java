package com.opsflow.auth.security;

import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = parseJwt(request);

        if (StringUtils.hasText(jwt) && jwtService.validateToken(jwt)) {
            String email = jwtService.getEmailFromToken(jwt);
            UUID userId = jwtService.getUserIdFromToken(jwt);
            UUID organizationId = jwtService.getOrganizationIdFromToken(jwt);
            Role role = jwtService.getRoleFromToken(jwt);

            // Re-check the account on every request: rejects tokens for
            // deactivated users and tokens issued before the last password
            // change/reset (passwordChangedAt invalidation).
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                filterChain.doFilter(request, response);
                return;
            }
            java.time.Instant issuedAt = jwtService.getIssuedAtFromToken(jwt);
            java.time.Instant passwordChangedAt = user.getPasswordChangedAt();
            if (issuedAt != null && passwordChangedAt != null && issuedAt.isBefore(passwordChangedAt)) {
                filterChain.doFilter(request, response);
                return;
            }

            UserPrincipal principal = new UserPrincipal(userId, organizationId, email, "", role, true);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
