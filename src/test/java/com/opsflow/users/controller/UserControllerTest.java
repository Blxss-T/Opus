package com.opsflow.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.dto.LoginRequest;
import com.opsflow.auth.security.JwtService;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.testsupport.TestDatabaseCleaner;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.dto.CreateUserRequest;
import com.opsflow.users.dto.UpdateUserRequest;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private User adminA;
    private User managerA;
    private User employeeA;
    private String adminAToken;
    private String managerAToken;
    private String employeeAToken;
    private String adminBToken;

    @BeforeEach
    void setUp() {
        // Reset the shared H2 test database in FK-safe order
        testDatabaseCleaner.clean();

        Organization orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        managerA = userRepository.save(new User(orgA, "manager@alpha.com", "hash", "Mana", "Alpha", Role.MANAGER));
        employeeA = userRepository.save(new User(orgA, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE));
        User adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        managerAToken = jwtService.generateToken(managerA);
        employeeAToken = jwtService.generateToken(employeeA);
        adminBToken = jwtService.generateToken(adminB);
    }

    @Test
    void orgAdminShouldCreateUserWhoCanThenLogIn() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "New.Manager@Alpha.com",
            "Password123!",
            "New",
            "Manager",
            Role.MANAGER
        );

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("new.manager@alpha.com"))
            .andExpect(jsonPath("$.data.role").value("MANAGER"))
            .andExpect(jsonPath("$.data.organizationId").value(adminA.getOrganization().getId().toString()))
            .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("new.manager@alpha.com", "Password123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.role").value("MANAGER"));
    }

    @Test
    void createShouldRejectAdminRolesAndDuplicateEmail() throws Exception {
        for (Role role : new Role[] {Role.ORG_ADMIN, Role.SUPER_ADMIN}) {
            CreateUserRequest request = new CreateUserRequest("second@alpha.com", "Password123!", "Second", "Admin", role);
            mockMvc.perform(post("/api/v1/users")
                    .header("Authorization", "Bearer " + adminAToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.error").value("ERR_422"));
        }

        // Emails are globally unique, so another organization's address is also taken
        CreateUserRequest duplicate = new CreateUserRequest("ADMIN@beta.com", "Password123!", "Dup", "User", Role.EMPLOYEE);
        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createShouldValidateBody() throws Exception {
        CreateUserRequest request = new CreateUserRequest("not-an-email", "short", "", "User", null);

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error").value("ERR_400"));
    }

    @Test
    void onlyOrgAdminShouldWriteAndEmployeeShouldNotRead() throws Exception {
        CreateUserRequest request = new CreateUserRequest("x@alpha.com", "Password123!", "X", "User", Role.EMPLOYEE);

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/users/" + employeeA.getId())
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + employeeAToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listShouldBeTenantScopedAndSupportFilters() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + managerAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(3)))
            .andExpect(jsonPath("$.data.page.totalElements").value(3));

        mockMvc.perform(get("/api/v1/users")
                .param("role", "EMPLOYEE")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].email").value("emp@alpha.com"));

        mockMvc.perform(get("/api/v1/users")
                .param("q", "MANAGER@")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].role").value("MANAGER"));

        mockMvc.perform(get("/api/v1/users")
                .param("role", "BOGUS")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void tenantIsolationShouldHideUsersFromOtherOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + employeeA.getId())
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/users/" + employeeA.getId())
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void roleChangeShouldApplyToAlreadyIssuedTokens() throws Exception {
        // EMPLOYEE cannot list users with their current token
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + employeeAToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/users/" + employeeA.getId())
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserRequest("Emma", null, Role.MANAGER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.firstName").value("Emma"))
            .andExpect(jsonPath("$.data.lastName").value("Alpha"))
            .andExpect(jsonPath("$.data.role").value("MANAGER"));

        // Same token, new role
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + employeeAToken))
            .andExpect(status().isOk());
    }

    @Test
    void orgAdminRoleCannotBeChangedOrAssignedOrDeactivated() throws Exception {
        mockMvc.perform(put("/api/v1/users/" + adminA.getId())
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, Role.EMPLOYEE))))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/v1/users/" + managerA.getId())
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateUserRequest(null, null, Role.ORG_ADMIN))))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/v1/users/" + adminA.getId())
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deactivatedUserShouldLoseAccessUntilReactivated() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateUserRequest("temp@alpha.com", "Password123!", "Temp", "User", Role.EMPLOYEE))))
            .andExpect(status().isCreated())
            .andReturn();
        String userId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();
        String loginBody = objectMapper.writeValueAsString(new LoginRequest("temp@alpha.com", "Password123!"));

        MvcResult session = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();
        String token = objectMapper.readTree(session.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                .param("active", "false")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(post("/api/v1/users/" + userId + "/activate")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk());
    }
}
