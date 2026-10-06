package com.opsflow.employees.repository;

import com.opsflow.employees.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Employee> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);
}
