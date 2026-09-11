package com.opsflow.employees.dto;

import com.opsflow.employees.domain.Employee;
import com.opsflow.employees.domain.EmploymentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String jobTitle,
    String department,
    EmploymentStatus employmentStatus,
    LocalDate hiredAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static EmployeeResponse fromEntity(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getOrganization().getId(),
            employee.getUser() != null ? employee.getUser().getId() : null,
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail(),
            employee.getJobTitle(),
            employee.getDepartment(),
            employee.getEmploymentStatus(),
            employee.getHiredAt(),
            employee.getCreatedAt(),
            employee.getUpdatedAt()
        );
    }
}
