package com.opsflow.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.dto.CreateProductRequest;
import com.opsflow.inventory.repository.InventoryMovementRepository;
import com.opsflow.inventory.repository.ProductRepository;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.opsflow.employees.repository.EmployeeRepository employeeRepository;

    @Autowired
    private ProductRepository productRepository;


    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

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
    void orgAdminShouldCreateProductSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
            "PRD-001",
            "Wireless Mouse",
            "Ergonomic 2.4GHz mouse",
            "Electronics",
            new BigDecimal("29.99"),
            new BigDecimal("12.50"),
            100,
            15
        );

        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sku").value("PRD-001"))
            .andExpect(jsonPath("$.data.stockQuantity").value(100));
    }

    @Test
    void employeeShouldBeForbiddenFromCreatingProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
            "PRD-002",
            "Keyboard",
            "Mechanical Keyboard",
            "Electronics",
            new BigDecimal("79.99"),
            new BigDecimal("35.00"),
            20,
            5
        );

        mockMvc.perform(post("/api/v1/products")
                .header("Authorization", "Bearer " + employeeAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAdjustStockAndRetrieveMovements() throws Exception {
        Product product = productRepository.save(new Product(
            orgA,
            "PRD-ADJUST",
            "Flash Drive",
            "64GB USB",
            "Electronics",
            new BigDecimal("15.00"),
            new BigDecimal("6.00"),
            50,
            10
        ));

        AdjustStockRequest adjustRequest = new AdjustStockRequest(25, MovementType.INBOUND, "Restock batch #123");

        mockMvc.perform(post("/api/v1/products/" + product.getId() + "/adjust-stock")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adjustRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.stockQuantity").value(75));

        mockMvc.perform(get("/api/v1/products/" + product.getId() + "/movements")
                .header("Authorization", "Bearer " + employeeAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].quantityDelta").value(25))
            .andExpect(jsonPath("$.data.content[0].movementType").value("INBOUND"));
    }

    @Test
    void tenantIsolationShouldPreventAccessFromOtherOrganization() throws Exception {
        Product productA = productRepository.save(new Product(
            orgA,
            "PRD-SECRET",
            "Alpha Proprietary Item",
            "Secret",
            "Hardware",
            new BigDecimal("999.00"),
            new BigDecimal("400.00"),
            10,
            2
        ));

        // User from Org B attempts to fetch Org A's product
        mockMvc.perform(get("/api/v1/products/" + productA.getId())
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isUnauthorized());
    }
}
