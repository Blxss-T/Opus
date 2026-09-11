package com.opsflow.employees.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.employees.domain.Employee;
import com.opsflow.employees.domain.EmploymentStatus;
import com.opsflow.employees.dto.CreateEmployeeRequest;
import com.opsflow.employees.dto.EmployeeResponse;
import com.opsflow.employees.dto.UpdateEmployeeRequest;
import com.opsflow.employees.repository.EmployeeRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public EmployeeService(
        EmployeeRepository employeeRepository,
        OrganizationRepository organizationRepository,
        UserRepository userRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(UserPrincipal principal, Pageable pageable) {
        return employeeRepository.findByOrganizationId(principal.getOrganizationId(), pageable)
            .map(EmployeeResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UserPrincipal principal, UUID id) {
        Employee employee = loadForAccess(principal, id);
        return EmployeeResponse.fromEntity(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getCurrent(UserPrincipal principal) {
        Employee employee = employeeRepository
            .findByUserIdAndOrganizationId(principal.getId(), principal.getOrganizationId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "userId", principal.getId()));
        return EmployeeResponse.fromEntity(employee);
    }

    @Transactional
    public EmployeeResponse create(UserPrincipal principal, CreateEmployeeRequest request) {
        requireRole(principal, Role.ORG_ADMIN);
        Organization organization = organizationRepository.findById(principal.getOrganizationId())
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", principal.getOrganizationId()));

        String email = request.email().toLowerCase(Locale.ROOT).trim();
        if (employeeRepository.existsByOrganizationIdAndEmail(organization.getId(), email)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "An employee with this email already exists in the organization");
        }

        Employee employee = new Employee();
        employee.setOrganization(organization);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setJobTitle(trimToNull(request.jobTitle()));
        employee.setDepartment(trimToNull(request.department()));
        employee.setEmploymentStatus(request.employmentStatus() != null ? request.employmentStatus() : EmploymentStatus.ACTIVE);
        employee.setHiredAt(request.hiredAt());
        employee.setUser(resolveLinkedUser(principal.getOrganizationId(), request.userId()));

        return EmployeeResponse.fromEntity(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(UserPrincipal principal, UUID id, UpdateEmployeeRequest request) {
        requireRole(principal, Role.ORG_ADMIN, Role.MANAGER);
        Employee employee = requireSameOrganization(principal, loadById(id));

        if (request.firstName() != null) {
            employee.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            employee.setLastName(request.lastName().trim());
        }
        if (request.email() != null) {
            String email = request.email().toLowerCase(Locale.ROOT).trim();
            if (!email.equals(employee.getEmail())
                && employeeRepository.existsByOrganizationIdAndEmail(employee.getOrganization().getId(), email)) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "An employee with this email already exists in the organization");
            }
            employee.setEmail(email);
        }
        if (request.jobTitle() != null) {
            employee.setJobTitle(trimToNull(request.jobTitle()));
        }
        if (request.department() != null) {
            employee.setDepartment(trimToNull(request.department()));
        }
        if (request.employmentStatus() != null) {
            employee.setEmploymentStatus(request.employmentStatus());
        }
        if (request.hiredAt() != null) {
            employee.setHiredAt(request.hiredAt());
        }
        if (request.userId() != null) {
            employee.setUser(resolveLinkedUser(principal.getOrganizationId(), request.userId()));
        }

        return EmployeeResponse.fromEntity(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse terminate(UserPrincipal principal, UUID id) {
        requireRole(principal, Role.ORG_ADMIN);
        Employee employee = requireSameOrganization(principal, loadById(id));
        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);
        return EmployeeResponse.fromEntity(employeeRepository.save(employee));
    }

    private Employee loadById(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private Employee loadForAccess(UserPrincipal principal, UUID id) {
        Employee employee = loadById(id);
        if (!employee.getOrganization().getId().equals(principal.getOrganizationId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied for requested operation");
        }
        if (principal.getRole() == Role.EMPLOYEE) {
            boolean ownsRecord = employee.getUser() != null && employee.getUser().getId().equals(principal.getId());
            if (!ownsRecord) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied for requested operation");
            }
        } else if (principal.getRole() != Role.ORG_ADMIN && principal.getRole() != Role.MANAGER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied for requested operation");
        }
        return employee;
    }

    private Employee requireSameOrganization(UserPrincipal principal, Employee employee) {
        if (!employee.getOrganization().getId().equals(principal.getOrganizationId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied for requested operation");
        }
        return employee;
    }

    private User resolveLinkedUser(UUID organizationId, UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot link a user from another organization");
        }
        return user;
    }

    private void requireRole(UserPrincipal principal, Role... allowed) {
        for (Role role : allowed) {
            if (principal.getRole() == role) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied for requested operation");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
