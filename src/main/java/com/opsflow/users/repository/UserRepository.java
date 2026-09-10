package com.opsflow.users.repository;

import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByOrganizationIdAndEmail(UUID organizationId, String email);

    boolean existsByOrganizationIdAndRole(UUID organizationId, Role role);
}

