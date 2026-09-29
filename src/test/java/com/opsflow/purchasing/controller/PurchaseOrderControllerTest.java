package com.opsflow.purchasing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.InventoryMovementRepository;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.purchasing.dto.CreatePurchaseOrderRequest;
import com.opsflow.purchasing.dto.PurchaseOrderItemRequest;
import com.opsflow.purchasing.dto.UpdatePurchaseOrderStatusRequest;
import com.opsflow.purchasing.repository.PurchaseOrderItemRepository;
import com.opsflow.purchasing.repository.PurchaseOrderRepository;
import com.opsflow.suppliers.domain.Supplier;
import com.opsflow.suppliers.repository.SupplierRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseOrderControllerTest {

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
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private Organization orgA;
    private Organization orgB;

    private User adminA;
    private User adminB;

    private String adminAToken;
    private String adminBToken;

    private Supplier supplierA;
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

        supplierA = supplierRepository.save(new Supplier(orgA, "Supplier Alpha", "Bob", "bob@supplier.com", null, null, null));
        productA = productRepository.save(new Product(
            orgA,
            "SKU-PO-1",
            "Bulk Cable",
            "Cat6 cable",
            "Cables",
            new BigDecimal("150.00"),
            new BigDecimal("80.00"),
            10,
            5
        ));
    }

    @Test
    void shouldCreatePOAndUpdateStatusToReceivedReplenishingInventory() throws Exception {
        // 1. Create Purchase Order
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest(productA.getId(), 20, new BigDecimal("80.00"));
        CreatePurchaseOrderRequest poRequest = new CreatePurchaseOrderRequest(
            supplierA.getId(),
            LocalDate.now().plusDays(5),
            "Stock replenishment",
            List.of(item)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/purchase-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(poRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.totalAmount").value(1600.0))
            .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        String poId = objectMapper.readTree(responseContent).path("data").path("id").asText();

        // 2. Update status: DRAFT -> ORDERED
        UpdatePurchaseOrderStatusRequest orderedRequest = new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.ORDERED);
        mockMvc.perform(patch("/api/v1/purchase-orders/" + poId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderedRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ORDERED"));

        // 3. Update status: ORDERED -> RECEIVED (should trigger inventory stock replenishment)
        UpdatePurchaseOrderStatusRequest receivedRequest = new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.RECEIVED);
        mockMvc.perform(patch("/api/v1/purchase-orders/" + poId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(receivedRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RECEIVED"));

        // 4. Verify product stock quantity in DB was increased from 10 to 30 (10 + 20)
        Product updatedProduct = productRepository.findById(productA.getId()).orElseThrow();
        assertEquals(30, updatedProduct.getStockQuantity());
    }

    @Test
    void tenantIsolationShouldPreventAccessFromOtherOrganization() throws Exception {
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest(productA.getId(), 5, new BigDecimal("80.00"));
        CreatePurchaseOrderRequest poRequest = new CreatePurchaseOrderRequest(
            supplierA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/purchase-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(poRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String poId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();

        // User from Org B attempts to access Org A's PO
        mockMvc.perform(get("/api/v1/purchase-orders/" + poId)
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isNotFound());
    }
}
