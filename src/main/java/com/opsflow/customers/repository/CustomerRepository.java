package com.opsflow.customers.repository;

import com.opsflow.customers.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByIdAndOrganizationIdAndActiveTrue(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

    boolean existsByOrganizationIdAndEmailAndIdNot(UUID organizationId, String email, UUID id);
}
