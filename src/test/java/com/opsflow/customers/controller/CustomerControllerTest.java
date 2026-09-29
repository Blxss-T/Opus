package com.opsflow.customers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.dto.CreateCustomerRequest;
import com.opsflow.customers.dto.UpdateCustomerRequest;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.testsupport.TestDatabaseCleaner;
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
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private com.opsflow.employees.repository.EmployeeRepository employeeRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private Organization orgA;
    private User adminA;
    private User employeeA;
    private User adminB;
    private String adminAToken;
    private String employeeAToken;
    private String adminBToken;

    @BeforeEach
    void setUp() {
        // Reset the shared H2 test database in FK-safe order
        testDatabaseCleaner.clean();

        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        employeeA = userRepository.save(new User(orgA, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE));
        adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        employeeAToken = jwtService.generateToken(employeeA);
        adminBToken = jwtService.generateToken(adminB);
    }

    @Test
    void orgAdminShouldCreateGetUpdateAndListCustomer() throws Exception {
        CreateCustomerRequest createRequest = new CreateCustomerRequest(
            "Wayne Enterprises",
            "bruce@wayne.com",
            "+1-555-0110",
            "Gotham",
            "Key account"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Wayne Enterprises"))
            .andExpect(jsonPath("$.data.email").value("bruce@wayne.com"))
            .andReturn();

        String customerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/customers/" + customerId)
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("bruce@wayne.com"));

        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
            "Wayne Enterprises International",
            "bruce@wayne.com",
            "+1-555-0111",
            "Gotham Tower",
            "VIP"
        );

        mockMvc.perform(put("/api/v1/customers/" + customerId)
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Wayne Enterprises International"));

        mockMvc.perform(get("/api/v1/customers")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    void unauthenticatedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeShouldBeForbiddenFromCreatingCustomer() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Wayne Enterprises",
            "bruce@wayne.com",
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "Bearer " + employeeAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailShouldReturnBusinessRuleViolation() throws Exception {
        customerRepository.save(new Customer(orgA, "Existing", "bruce@wayne.com", null, null, null));

        CreateCustomerRequest request = new CreateCustomerRequest(
            "Clone Co",
            "BRUCE@wayne.com",
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.error").value("ERR_422"));
    }

    @Test
    void tenantIsolationShouldPreventAccessFromOtherOrganization() throws Exception {
        Customer customerA = customerRepository.save(
            new Customer(orgA, "Alpha Customer", "cust@alpha.com", null, null, null)
        );

        mockMvc.perform(get("/api/v1/customers/" + customerA.getId())
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void softDeletedCustomerShouldDisappearFromListAndGet() throws Exception {
        Customer customer = customerRepository.save(
            new Customer(orgA, "Temp Customer", "temp@alpha.com", null, null, null)
        );

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId())
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/customers")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/customers/" + customer.getId())
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isNotFound());
    }
}
