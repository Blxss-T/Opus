package com.opsflow.suppliers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.suppliers.domain.Supplier;
import com.opsflow.suppliers.dto.CreateSupplierRequest;
import com.opsflow.suppliers.repository.SupplierRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private com.opsflow.employees.repository.EmployeeRepository employeeRepository;

    @Autowired
    private com.opsflow.inventory.repository.InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private com.opsflow.inventory.repository.ProductRepository productRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private Organization orgA;
    private Organization orgB;

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
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        employeeA = userRepository.save(new User(orgA, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE));
        adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        employeeAToken = jwtService.generateToken(employeeA);
        adminBToken = jwtService.generateToken(adminB);
    }

    @Test
    void orgAdminShouldCreateSupplierSuccessfully() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "Apex Logistics",
            "John Doe",
            "john@apex.com",
            "+1-555-0199",
            "456 Warehouse Ave",
            "TX-998811"
        );

        mockMvc.perform(post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Apex Logistics"))
            .andExpect(jsonPath("$.data.contactPerson").value("John Doe"));
    }

    @Test
    void searchShouldFilterByNameEmailOrContactPerson() throws Exception {
        supplierRepository.save(new Supplier(orgA, "Apex Logistics", "John Doe", "john@apex.com", null, null, null));
        supplierRepository.save(new Supplier(orgA, "Globex Freight", "Jane Roe", "jane@globex.com", null, null, null));

        mockMvc.perform(get("/api/v1/suppliers")
                .param("q", "jane")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].name").value("Globex Freight"));

        mockMvc.perform(get("/api/v1/suppliers")
                .param("q", "apex")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].email").value("john@apex.com"));
    }

    @Test
    void employeeShouldBeForbiddenFromCreatingSupplier() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "Apex Logistics",
            "John Doe",
            "john@apex.com",
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/suppliers")
                .header("Authorization", "Bearer " + employeeAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void tenantIsolationShouldPreventAccessFromOtherOrganization() throws Exception {
        Supplier supplierA = supplierRepository.save(new Supplier(
            orgA,
            "Alpha Exclusive Supplier",
            null,
            null,
            null,
            null,
            null
        ));

        mockMvc.perform(get("/api/v1/suppliers/" + supplierA.getId())
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }
}
