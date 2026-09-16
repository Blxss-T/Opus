package com.opsflow.inventory.repository;

import com.opsflow.inventory.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Product> findByIdAndOrganizationIdAndActiveTrue(UUID id, UUID organizationId);

    Page<Product> findByOrganizationIdAndActiveTrue(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    boolean existsByOrganizationIdAndSkuIgnoreCaseAndIdNot(UUID organizationId, String sku, UUID id);
}
