package com.opsflow.purchasing.repository;

import com.opsflow.purchasing.domain.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Optional<PurchaseOrder> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<PurchaseOrder> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);
}
