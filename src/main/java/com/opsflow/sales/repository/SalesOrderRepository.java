package com.opsflow.sales.repository;

import com.opsflow.sales.domain.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    Optional<SalesOrder> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<SalesOrder> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);
}
