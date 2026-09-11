package com.opsflow.employees.dto;

import com.opsflow.employees.domain.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email String email,
    @Size(max = 150) String jobTitle,
    @Size(max = 150) String department,
    EmploymentStatus employmentStatus,
    LocalDate hiredAt,
    UUID userId
) {}
