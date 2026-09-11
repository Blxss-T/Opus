package com.opsflow.employees.repository;

import com.opsflow.employees.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Page<Employee> findByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Employee> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);
}
