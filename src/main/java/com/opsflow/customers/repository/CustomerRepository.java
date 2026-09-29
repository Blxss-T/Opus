package com.opsflow.customers.repository;

import com.opsflow.customers.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndOrganizationIdAndActiveTrue(UUID id, UUID organizationId);

    Page<Customer> findByOrganizationIdAndActiveTrue(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

    boolean existsByOrganizationIdAndEmailAndIdNot(UUID organizationId, String email, UUID id);
}
