package com.opsflow.inventory.repository;

import com.opsflow.inventory.domain.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findByOrganizationIdAndProductIdOrderByCreatedAtDesc(
        UUID organizationId,
        UUID productId,
        Pageable pageable
    );
}
