package com.opsflow.sales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Editing draft sales and purchase orders through PUT /{id}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderDraftEditTest {

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
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private String adminToken;
    private String employeeToken;
    private String otherOrgToken;
    private Customer customerOne;
    private Customer customerTwo;
    private Supplier supplierOne;
    private Supplier supplierTwo;
    private Product widget;
    private Product gadget;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();

        Organization org = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization otherOrg = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));
        adminToken = jwtService.generateToken(
            userRepository.save(new User(org, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN)));
        employeeToken = jwtService.generateToken(
            userRepository.save(new User(org, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE)));
        otherOrgToken = jwtService.generateToken(
            userRepository.save(new User(otherOrg, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN)));

        customerOne = customerRepository.save(new Customer(org, "Customer One", "one@cust.com", null, null, null));
        customerTwo = customerRepository.save(new Customer(org, "Customer Two", "two@cust.com", null, null, null));
        supplierOne = supplierRepository.save(new Supplier(org, "Supplier One", null, "one@supp.com", null, null, null));
        supplierTwo = supplierRepository.save(new Supplier(org, "Supplier Two", null, "two@supp.com", null, null, null));
        widget = productRepository.save(new Product(org, "W-001", "Widget", null, "Widgets",
            new BigDecimal("10.00"), new BigDecimal("6.00"), 50, 10));
        gadget = productRepository.save(new Product(org, "G-001", "Gadget", null, "Gadgets",
            new BigDecimal("25.00"), new BigDecimal("15.00"), 50, 10));
    }

    @Test
    void shouldReplaceTheContentsOfADraftSalesOrder() throws Exception {
        String id = createSalesOrder(salesOrderJson(customerOne, widget, 2, "10.00", "original"));

        mockMvc.perform(put("/api/v1/sales-orders/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerTwo.getId() + "\",\"notes\":\"revised\","
                    + "\"expectedDeliveryDate\":\"2027-01-15\",\"items\":["
                    + "{\"productId\":\"" + widget.getId() + "\",\"quantity\":3,\"unitPrice\":10.00},"
                    + "{\"productId\":\"" + gadget.getId() + "\",\"quantity\":1,\"unitPrice\":25.00}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.notes").value("revised"))
            .andExpect(jsonPath("$.data.expectedDeliveryDate").value("2027-01-15"))
            .andExpect(jsonPath("$.data.totalAmount").value(55.00))
            .andExpect(jsonPath("$.data.items", hasSize(2)));

        mockMvc.perform(get("/api/v1/sales-orders/" + id).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items", hasSize(2)))
            .andExpect(jsonPath("$.data.totalAmount").value(55.00));

        // The replaced line is gone rather than orphaned.
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sales_order_items", Integer.class));
    }

    @Test
    void shouldReplaceTheContentsOfADraftPurchaseOrder() throws Exception {
        String id = createPurchaseOrder(purchaseOrderJson(supplierOne, widget, 5, "6.00"));

        mockMvc.perform(put("/api/v1/purchase-orders/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchaseOrderJson(supplierTwo, gadget, 4, "15.00")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.totalAmount").value(60.00))
            .andExpect(jsonPath("$.data.items", hasSize(1)));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM purchase_order_items", Integer.class));
    }

    @Test
    void ordersThatHaveLeftDraftShouldNotBeEditable() throws Exception {
        String salesOrderId = createSalesOrder(salesOrderJson(customerOne, widget, 2, "10.00", "original"));
        mockMvc.perform(patch("/api/v1/sales-orders/" + salesOrderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/sales-orders/" + salesOrderId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(salesOrderJson(customerTwo, gadget, 9, "25.00", "too late")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.message").value("Only DRAFT sales orders can be edited; this one is CONFIRMED"));

        String purchaseOrderId = createPurchaseOrder(purchaseOrderJson(supplierOne, widget, 5, "6.00"));
        mockMvc.perform(patch("/api/v1/purchase-orders/" + purchaseOrderId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/purchase-orders/" + purchaseOrderId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchaseOrderJson(supplierTwo, gadget, 4, "15.00")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.message").value("Only DRAFT purchase orders can be edited; this one is CANCELLED"));
    }

    @Test
    void editShouldEnforceRolesTenantIsolationAndValidation() throws Exception {
        String id = createSalesOrder(salesOrderJson(customerOne, widget, 2, "10.00", "original"));
        String body = salesOrderJson(customerTwo, gadget, 1, "25.00", "revised");

        mockMvc.perform(put("/api/v1/sales-orders/" + id)
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/sales-orders/" + id)
                .header("Authorization", "Bearer " + otherOrgToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/sales-orders/" + id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + customerOne.getId() + "\",\"items\":[]}"))
            .andExpect(status().isBadRequest());

        // A rejected edit leaves the order as it was.
        mockMvc.perform(get("/api/v1/sales-orders/" + id).header("Authorization", "Bearer " + adminToken))
            .andExpect(jsonPath("$.data.notes").value("original"))
            .andExpect(jsonPath("$.data.items", hasSize(1)))
            .andExpect(jsonPath("$.data.totalAmount").value(20.00));
    }

    private String createSalesOrder(String body) throws Exception {
        return create("/api/v1/sales-orders", body);
    }

    private String createPurchaseOrder(String body) throws Exception {
        return create("/api/v1/purchase-orders", body);
    }

    private String create(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asText();
    }

    private String salesOrderJson(Customer customer, Product product, int quantity, String unitPrice, String notes) {
        return "{\"customerId\":\"" + customer.getId() + "\",\"notes\":\"" + notes + "\",\"items\":[{\"productId\":\""
            + product.getId() + "\",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}]}";
    }

    private String purchaseOrderJson(Supplier supplier, Product product, int quantity, String unitCost) {
        return "{\"supplierId\":\"" + supplier.getId() + "\",\"items\":[{\"productId\":\""
            + product.getId() + "\",\"quantity\":" + quantity + ",\"unitCost\":" + unitCost + "}]}";
    }
}
