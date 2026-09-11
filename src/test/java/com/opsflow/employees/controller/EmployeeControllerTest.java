package com.opsflow.employees.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.employees.domain.Employee;
import com.opsflow.employees.domain.EmploymentStatus;
import com.opsflow.employees.dto.CreateEmployeeRequest;
import com.opsflow.employees.dto.UpdateEmployeeRequest;
import com.opsflow.employees.repository.EmployeeRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtService jwtService;

    private Organization orgA;
    private Organization orgB;

    private User adminA;
    private User managerA;
    private User employeeA;
    private User adminB;

    private String tokenAdminA;
    private String tokenManagerA;
    private String tokenEmployeeA;
    private String tokenAdminB;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.test", "pass", "Admin", "A", Role.ORG_ADMIN));
        managerA = userRepository.save(new User(orgA, "manager@alpha.test", "pass", "Manager", "A", Role.MANAGER));
        employeeA = userRepository.save(new User(orgA, "emp@alpha.test", "pass", "Employee", "A", Role.EMPLOYEE));
        adminB = userRepository.save(new User(orgB, "admin@beta.test", "pass", "Admin", "B", Role.ORG_ADMIN));

        tokenAdminA = jwtService.generateToken(adminA);
        tokenManagerA = jwtService.generateToken(managerA);
        tokenEmployeeA = jwtService.generateToken(employeeA);
        tokenAdminB = jwtService.generateToken(adminB);
    }

    @Test
    void shouldAllowOrgAdminFullEmployeeLifecycle() throws Exception {
        CreateEmployeeRequest createRequest = new CreateEmployeeRequest(
            "Linus", "Torvalds", "linus@alpha.test", "Kernel Lead", "Core",
            EmploymentStatus.ACTIVE, LocalDate.of(2025, 1, 1), null
        );

        // 1. Create Employee
        String createJson = mockMvc.perform(post("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenAdminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("linus@alpha.test"))
            .andExpect(jsonPath("$.data.employmentStatus").value("ACTIVE"))
            .andReturn().getResponse().getContentAsString();

        String employeeId = objectMapper.readTree(createJson).path("data").path("id").asText();

        // 2. List Employees
        mockMvc.perform(get("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].id").value(employeeId));

        // 3. Get Employee By ID
        mockMvc.perform(get("/api/v1/employees/" + employeeId)
                .header("Authorization", "Bearer " + tokenAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(employeeId));

        // 4. Update Employee
        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
            null, null, null, "Principal Engineer", "Systems", null, null, null
        );
        mockMvc.perform(put("/api/v1/employees/" + employeeId)
                .header("Authorization", "Bearer " + tokenAdminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.jobTitle").value("Principal Engineer"));

        // 5. Terminate Employee
        mockMvc.perform(delete("/api/v1/employees/" + employeeId)
                .header("Authorization", "Bearer " + tokenAdminA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.employmentStatus").value("TERMINATED"));
    }

    @Test
    void shouldEnforceManagerPermissions() throws Exception {
        Employee emp = new Employee();
        emp.setOrganization(orgA);
        emp.setFirstName("Dan");
        emp.setLastName("Ingalls");
        emp.setEmail("dan@alpha.test");
        emp.setJobTitle("Smalltalk Dev");
        emp.setEmploymentStatus(EmploymentStatus.ACTIVE);
        emp = employeeRepository.save(emp);

        // Manager can list
        mockMvc.perform(get("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenManagerA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Manager can update
        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
            null, null, null, "Senior Smalltalk Dev", null, null, null, null
        );
        mockMvc.perform(put("/api/v1/employees/" + emp.getId())
                .header("Authorization", "Bearer " + tokenManagerA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.jobTitle").value("Senior Smalltalk Dev"));

        // Manager CANNOT create (403)
        CreateEmployeeRequest createRequest = new CreateEmployeeRequest(
            "Adele", "Goldberg", "adele@alpha.test", "Dev", "UI",
            EmploymentStatus.ACTIVE, null, null
        );
        mockMvc.perform(post("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenManagerA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isForbidden());

        // Manager CANNOT terminate (403)
        mockMvc.perform(delete("/api/v1/employees/" + emp.getId())
                .header("Authorization", "Bearer " + tokenManagerA))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceEmployeeRoleRestrictionsAndAllowSelfAccess() throws Exception {
        // Linked employee record for employeeA
        Employee ownRecord = new Employee();
        ownRecord.setOrganization(orgA);
        ownRecord.setUser(employeeA);
        ownRecord.setFirstName("Employee");
        ownRecord.setLastName("A");
        ownRecord.setEmail(employeeA.getEmail());
        ownRecord.setEmploymentStatus(EmploymentStatus.ACTIVE);
        ownRecord = employeeRepository.save(ownRecord);

        // Unlinked employee in orgA
        Employee otherRecord = new Employee();
        otherRecord.setOrganization(orgA);
        otherRecord.setFirstName("Other");
        otherRecord.setLastName("Person");
        otherRecord.setEmail("other@alpha.test");
        otherRecord.setEmploymentStatus(EmploymentStatus.ACTIVE);
        otherRecord = employeeRepository.save(otherRecord);

        // Employee can access /me
        mockMvc.perform(get("/api/v1/employees/me")
                .header("Authorization", "Bearer " + tokenEmployeeA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(ownRecord.getId().toString()))
            .andExpect(jsonPath("$.data.email").value(employeeA.getEmail()));

        // Employee can access own record by ID
        mockMvc.perform(get("/api/v1/employees/" + ownRecord.getId())
                .header("Authorization", "Bearer " + tokenEmployeeA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(ownRecord.getId().toString()));

        // Employee CANNOT list all employees (403)
        mockMvc.perform(get("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenEmployeeA))
            .andExpect(status().isForbidden());

        // Employee CANNOT view other employee by ID (403)
        mockMvc.perform(get("/api/v1/employees/" + otherRecord.getId())
                .header("Authorization", "Bearer " + tokenEmployeeA))
            .andExpect(status().isForbidden());

        // Employee CANNOT create employee (403)
        mockMvc.perform(post("/api/v1/employees")
                .header("Authorization", "Bearer " + tokenEmployeeA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                    "New", "Guy", "new@alpha.test", "Dev", "Eng", EmploymentStatus.ACTIVE, null, null
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventCrossTenantIdorAccess() throws Exception {
        // Employee in Org A
        Employee empInA = new Employee();
        empInA.setOrganization(orgA);
        empInA.setFirstName("Alice");
        empInA.setLastName("A");
        empInA.setEmail("alice@alpha.test");
        empInA.setEmploymentStatus(EmploymentStatus.ACTIVE);
        empInA = employeeRepository.save(empInA);

        // Admin of Org B attempts to read employee in Org A -> 403 Forbidden
        mockMvc.perform(get("/api/v1/employees/" + empInA.getId())
                .header("Authorization", "Bearer " + tokenAdminB))
            .andExpect(status().isForbidden());

        // Admin of Org B attempts to update employee in Org A -> 403 Forbidden
        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
            null, null, null, "Hacked Job", null, null, null, null
        );
        mockMvc.perform(put("/api/v1/employees/" + empInA.getId())
                .header("Authorization", "Bearer " + tokenAdminB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());

        // Admin of Org B attempts to terminate employee in Org A -> 403 Forbidden
        mockMvc.perform(delete("/api/v1/employees/" + empInA.getId())
                .header("Authorization", "Bearer " + tokenAdminB))
            .andExpect(status().isForbidden());
    }
}
