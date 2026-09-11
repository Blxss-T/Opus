package com.opsflow.employees.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    private EmployeeService employeeService;

    private UUID orgId1;
    private UUID orgId2;
    private Organization org1;
    private Organization org2;
    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal otherOrgAdminPrincipal;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, organizationRepository, userRepository);

        orgId1 = UUID.randomUUID();
        orgId2 = UUID.randomUUID();

        org1 = new Organization("Org 1", "org-1");
        org1.setId(orgId1);

        org2 = new Organization("Org 2", "org-2");
        org2.setId(orgId2);

        adminPrincipal = new UserPrincipal(UUID.randomUUID(), orgId1, "admin@org1.test", "hash", Role.ORG_ADMIN, true);
        managerPrincipal = new UserPrincipal(UUID.randomUUID(), orgId1, "manager@org1.test", "hash", Role.MANAGER, true);
        employeePrincipal = new UserPrincipal(UUID.randomUUID(), orgId1, "emp@org1.test", "hash", Role.EMPLOYEE, true);
        otherOrgAdminPrincipal = new UserPrincipal(UUID.randomUUID(), orgId2, "admin@org2.test", "hash", Role.ORG_ADMIN, true);
    }

    @Test
    void shouldCreateEmployeeWhenCallerIsOrgAdmin() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "Grace", "Hopper", "grace@org1.test", "Lead Architect", "Engineering",
            EmploymentStatus.ACTIVE, LocalDate.of(2025, 1, 15), null
        );

        when(organizationRepository.findById(orgId1)).thenReturn(Optional.of(org1));
        when(employeeRepository.existsByOrganizationIdAndEmail(orgId1, "grace@org1.test")).thenReturn(false);

        Employee saved = new Employee();
        saved.setId(UUID.randomUUID());
        saved.setOrganization(org1);
        saved.setFirstName("Grace");
        saved.setLastName("Hopper");
        saved.setEmail("grace@org1.test");
        saved.setJobTitle("Lead Architect");
        saved.setDepartment("Engineering");
        saved.setEmploymentStatus(EmploymentStatus.ACTIVE);
        saved.setHiredAt(LocalDate.of(2025, 1, 15));

        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponse response = employeeService.create(adminPrincipal, request);

        assertNotNull(response);
        assertEquals("grace@org1.test", response.email());
        assertEquals("Grace", response.firstName());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldRejectCreateEmployeeWhenCallerIsNotOrgAdmin() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "Grace", "Hopper", "grace@org1.test", "Lead Architect", "Engineering",
            EmploymentStatus.ACTIVE, LocalDate.of(2025, 1, 15), null
        );

        BusinessException ex = assertThrows(BusinessException.class,
            () -> employeeService.create(managerPrincipal, request));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void shouldRejectDuplicateEmployeeEmailWithinOrganization() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "Grace", "Hopper", "grace@org1.test", "Lead Architect", "Engineering",
            EmploymentStatus.ACTIVE, LocalDate.of(2025, 1, 15), null
        );

        when(organizationRepository.findById(orgId1)).thenReturn(Optional.of(org1));
        when(employeeRepository.existsByOrganizationIdAndEmail(orgId1, "grace@org1.test")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> employeeService.create(adminPrincipal, request));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
    }

    @Test
    void shouldRejectLinkingUserFromDifferentOrganization() {
        UUID crossOrgUserId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            "Grace", "Hopper", "grace@org1.test", "Lead Architect", "Engineering",
            EmploymentStatus.ACTIVE, LocalDate.of(2025, 1, 15), crossOrgUserId
        );

        User crossOrgUser = new User(org2, "cross@org2.test", "pass", "Cross", "User", Role.EMPLOYEE);
        crossOrgUser.setId(crossOrgUserId);

        when(organizationRepository.findById(orgId1)).thenReturn(Optional.of(org1));
        when(employeeRepository.existsByOrganizationIdAndEmail(orgId1, "grace@org1.test")).thenReturn(false);
        when(userRepository.findById(crossOrgUserId)).thenReturn(Optional.of(crossOrgUser));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> employeeService.create(adminPrincipal, request));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void shouldPreventCrossOrgIdorOnGetById() {
        UUID employeeId = UUID.randomUUID();
        Employee employeeInOrg1 = new Employee();
        employeeInOrg1.setId(employeeId);
        employeeInOrg1.setOrganization(org1);
        employeeInOrg1.setEmail("emp@org1.test");
        employeeInOrg1.setFirstName("Emp");
        employeeInOrg1.setLastName("One");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employeeInOrg1));

        // Caller from Org2 attempts to fetch employee from Org1
        BusinessException ex = assertThrows(BusinessException.class,
            () -> employeeService.getById(otherOrgAdminPrincipal, employeeId));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void shouldPreventEmployeeFromViewingOtherEmployees() {
        UUID otherEmployeeId = UUID.randomUUID();
        User otherUser = new User(org1, "other@org1.test", "pass", "Other", "User", Role.EMPLOYEE);
        otherUser.setId(UUID.randomUUID());

        Employee otherEmployee = new Employee();
        otherEmployee.setId(otherEmployeeId);
        otherEmployee.setOrganization(org1);
        otherEmployee.setUser(otherUser);
        otherEmployee.setEmail("other@org1.test");
        otherEmployee.setFirstName("Other");
        otherEmployee.setLastName("User");

        when(employeeRepository.findById(otherEmployeeId)).thenReturn(Optional.of(otherEmployee));

        // employeePrincipal attempts to read otherEmployee
        BusinessException ex = assertThrows(BusinessException.class,
            () -> employeeService.getById(employeePrincipal, otherEmployeeId));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void shouldAllowEmployeeToViewOwnRecord() {
        UUID ownEmployeeId = UUID.randomUUID();
        User ownUser = new User(org1, employeePrincipal.getUsername(), "pass", "Self", "User", Role.EMPLOYEE);
        ownUser.setId(employeePrincipal.getId());

        Employee ownEmployee = new Employee();
        ownEmployee.setId(ownEmployeeId);
        ownEmployee.setOrganization(org1);
        ownEmployee.setUser(ownUser);
        ownEmployee.setEmail(employeePrincipal.getUsername());
        ownEmployee.setFirstName("Self");
        ownEmployee.setLastName("User");

        when(employeeRepository.findById(ownEmployeeId)).thenReturn(Optional.of(ownEmployee));

        EmployeeResponse response = employeeService.getById(employeePrincipal, ownEmployeeId);

        assertNotNull(response);
        assertEquals(employeePrincipal.getUsername(), response.email());
    }

    @Test
    void shouldAllowOrgAdminAndManagerToUpdateEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setOrganization(org1);
        employee.setEmail("dev@org1.test");
        employee.setFirstName("Dev");
        employee.setLastName("One");
        employee.setJobTitle("Junior Engineer");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
            null, null, null, "Senior Engineer", "Platform", null, null, null
        );

        EmployeeResponse response = employeeService.update(managerPrincipal, employeeId, updateRequest);

        assertNotNull(response);
        assertEquals("Senior Engineer", response.jobTitle());
        assertEquals("Platform", response.department());
    }

    @Test
    void shouldTerminateEmployeeWhenCallerIsOrgAdmin() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setOrganization(org1);
        employee.setEmail("dev@org1.test");
        employee.setFirstName("Dev");
        employee.setLastName("One");
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.terminate(adminPrincipal, employeeId);

        assertNotNull(response);
        assertEquals(EmploymentStatus.TERMINATED, response.employmentStatus());
    }

    @Test
    void shouldListEmployeesScopedToOrganization() {
        Employee e1 = new Employee();
        e1.setId(UUID.randomUUID());
        e1.setOrganization(org1);
        e1.setEmail("e1@org1.test");
        e1.setFirstName("E1");
        e1.setLastName("Test");

        PageRequest pageable = PageRequest.of(0, 10);
        when(employeeRepository.findByOrganizationId(orgId1, pageable))
            .thenReturn(new PageImpl<>(List.of(e1), pageable, 1));

        Page<EmployeeResponse> page = employeeService.list(adminPrincipal, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("e1@org1.test", page.getContent().get(0).email());
    }
}
