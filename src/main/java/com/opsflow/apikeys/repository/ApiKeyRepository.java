package com.opsflow.apikeys.repository;

import com.opsflow.apikeys.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    Optional<ApiKey> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ApiKey> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
