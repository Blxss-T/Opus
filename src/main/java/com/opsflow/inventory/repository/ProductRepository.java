package com.opsflow.inventory.repository;

import com.opsflow.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Product> findByIdAndOrganizationIdAndActiveTrue(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    boolean existsByOrganizationIdAndSkuIgnoreCaseAndIdNot(UUID organizationId, String sku, UUID id);

    @Query(
        "SELECT p FROM Product p " +
            "WHERE p.organization.id = :organizationId " +
            "AND p.active = true AND p.stockQuantity <= p.reorderLevel " +
            "ORDER BY (p.stockQuantity - p.reorderLevel) ASC, p.name ASC"
    )
    List<Product> findByOrganizationIdAndLowStock(@Param("organizationId") UUID organizationId);
}
