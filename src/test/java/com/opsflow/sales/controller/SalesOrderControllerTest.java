package com.opsflow.sales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.InventoryMovementRepository;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.sales.domain.SalesOrderStatus;
import com.opsflow.sales.dto.CreateSalesOrderRequest;
import com.opsflow.sales.dto.SalesOrderItemRequest;
import com.opsflow.sales.dto.UpdateSalesOrderStatusRequest;
import com.opsflow.sales.repository.SalesOrderItemRepository;
import com.opsflow.sales.repository.SalesOrderRepository;
import com.opsflow.testsupport.TestDatabaseCleaner;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesOrderControllerTest {

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
    private TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    private JwtService jwtService;

    private Organization orgA;
    private Organization orgB;

    private User adminA;
    private User adminB;

    private String adminAToken;
    private String adminBToken;

    private Customer customerA;
    private Product productA;

    @BeforeEach
    void setUp() {
        // Reset the shared H2 test database in FK-safe order
        testDatabaseCleaner.clean();

        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        adminBToken = jwtService.generateToken(adminB);

        customerA = customerRepository.save(new Customer(orgA, "Customer Alpha", "alpha@customer.com", null, null, null));
        productA = productRepository.save(new Product(
            orgA,
            "SKU-SO-1",
            "Bulk Cable",
            "Cat6 cable",
            "Cables",
            new BigDecimal("150.00"),
            new BigDecimal("80.00"),
            100,
            5
        ));
    }

    @Test
    void shouldCreateSOAndUpdateStatusToFulfilledDepletingInventory() throws Exception {
        // 1. Create Sales Order
        SalesOrderItemRequest item = new SalesOrderItemRequest(productA.getId(), 20, new BigDecimal("150.00"));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            LocalDate.now().plusDays(5),
            "Customer order",
            List.of(item)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.totalAmount").value(3000.0))
            .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String soId = objectMapper.readTree(responseContent).path("data").path("id").asText();

        // 2. Update status: DRAFT -> CONFIRMED
        UpdateSalesOrderStatusRequest confirmedRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.CONFIRMED);
        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmedRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // 3. Update status: CONFIRMED -> FULFILLED (should trigger inventory depletion)
        UpdateSalesOrderStatusRequest fulfilledRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.FULFILLED);
        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fulfilledRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("FULFILLED"));

        // 4. Verify product stock quantity in DB was decreased from 100 to 80 (100 - 20)
        Product updatedProduct = productRepository.findById(productA.getId()).orElseThrow();
        assertEquals(80, updatedProduct.getStockQuantity());
    }

    @Test
    void tenantIsolationShouldPreventAccessFromOtherOrganization() throws Exception {
        SalesOrderItemRequest item = new SalesOrderItemRequest(productA.getId(), 5, new BigDecimal("150.00"));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String soId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();

        // User from Org B attempts to access Org A's SO
        mockMvc.perform(get("/api/v1/sales-orders/" + soId)
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void listShouldFilterByStatusAndRejectInvalidValues() throws Exception {
        SalesOrderItemRequest item = new SalesOrderItemRequest(productA.getId(), 5, new BigDecimal("150.00"));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult first = mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated());

        String soId = objectMapper.readTree(first.getResponse().getContentAsString()).path("data").path("id").asText();
        UpdateSalesOrderStatusRequest confirmed = new UpdateSalesOrderStatusRequest(SalesOrderStatus.CONFIRMED);
        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmed)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sales-orders")
                .param("status", "CONFIRMED")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/sales-orders")
                .param("status", "DRAFT")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/sales-orders")
                .param("status", "NOT_A_STATUS")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error").value("ERR_400"));
    }

    @Test
    void fulfillmentShouldFailWhenInsufficientStock() throws Exception {
        // Product starts with 100 units; order more than available
        SalesOrderItemRequest item = new SalesOrderItemRequest(productA.getId(), 150, new BigDecimal("150.00"));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String soId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();

        // DRAFT -> CONFIRMED
        UpdateSalesOrderStatusRequest confirmedRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.CONFIRMED);
        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmedRequest)))
            .andExpect(status().isOk());

        // CONFIRMED -> FULFILLED should fail with a business rule violation
        UpdateSalesOrderStatusRequest fulfilledRequest = new UpdateSalesOrderStatusRequest(SalesOrderStatus.FULFILLED);
        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fulfilledRequest)))
            .andExpect(status().isUnprocessableEntity());

        // Stock must be unchanged and status still CONFIRMED
        Product updatedProduct = productRepository.findById(productA.getId()).orElseThrow();
        assertEquals(100, updatedProduct.getStockQuantity());

        mockMvc.perform(get("/api/v1/sales-orders/" + soId)
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
