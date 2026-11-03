package com.opsflow.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.purchasing.repository.PurchaseOrderRepository;
import com.opsflow.sales.repository.SalesOrderRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIdempotencyTest {

    private static final String HEADER = "Idempotency-Key";

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
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private String tokenA;
    private String tokenB;
    private String salesOrderBodyA;
    private String salesOrderBodyB;
    private String purchaseOrderBodyA;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();

        Organization orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        Organization orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));
        tokenA = jwtService.generateToken(
            userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN)));
        tokenB = jwtService.generateToken(
            userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN)));

        Product productA = product(orgA);
        Product productB = product(orgB);
        Customer customerA = customerRepository.save(new Customer(orgA, "Cust A", "a@cust.com", null, null, null));
        Customer customerB = customerRepository.save(new Customer(orgB, "Cust B", "b@cust.com", null, null, null));
        Supplier supplierA = supplierRepository.save(new Supplier(orgA, "Supp A", null, "a@supp.com", null, null, null));

        salesOrderBodyA = "{\"customerId\":\"" + customerA.getId() + "\",\"items\":[{\"productId\":\""
            + productA.getId() + "\",\"quantity\":2,\"unitPrice\":10.00}]}";
        salesOrderBodyB = "{\"customerId\":\"" + customerB.getId() + "\",\"items\":[{\"productId\":\""
            + productB.getId() + "\",\"quantity\":2,\"unitPrice\":10.00}]}";
        purchaseOrderBodyA = "{\"supplierId\":\"" + supplierA.getId() + "\",\"items\":[{\"productId\":\""
            + productA.getId() + "\",\"quantity\":5,\"unitCost\":6.00}]}";
    }

    @Test
    void retryingASalesOrderWithTheSameKeyShouldReturnTheOriginalOrder() throws Exception {
        String firstId = idOf(mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "retry-1"))
            .andExpect(status().isCreated())
            .andExpect(header().doesNotExist("Idempotency-Replayed"))
            .andReturn());

        String replayedId = idOf(mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "retry-1"))
            .andExpect(status().isOk())
            .andExpect(header().string("Idempotency-Replayed", "true"))
            .andExpect(jsonPath("$.data.soNumber").isNotEmpty())
            .andReturn());

        assertEquals(firstId, replayedId);
        assertEquals(1, salesOrderRepository.count());
    }

    @Test
    void retryingAPurchaseOrderWithTheSameKeyShouldReturnTheOriginalOrder() throws Exception {
        String firstId = idOf(mockMvc.perform(create("/api/v1/purchase-orders", tokenA, purchaseOrderBodyA, "po-retry"))
            .andExpect(status().isCreated())
            .andReturn());

        String replayedId = idOf(mockMvc.perform(create("/api/v1/purchase-orders", tokenA, purchaseOrderBodyA, "po-retry"))
            .andExpect(status().isOk())
            .andExpect(header().string("Idempotency-Replayed", "true"))
            .andReturn());

        assertEquals(firstId, replayedId);
        assertEquals(1, purchaseOrderRepository.count());
    }

    @Test
    void differentOrMissingKeysShouldCreateSeparateOrders() throws Exception {
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "key-1")).andExpect(status().isCreated());
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "key-2")).andExpect(status().isCreated());
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, null)).andExpect(status().isCreated());
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, null)).andExpect(status().isCreated());

        assertEquals(4, salesOrderRepository.count());
    }

    @Test
    void theSameKeyInAnotherOrganizationShouldNotCollide() throws Exception {
        String idA = idOf(mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "shared-key"))
            .andExpect(status().isCreated())
            .andReturn());
        String idB = idOf(mockMvc.perform(create("/api/v1/sales-orders", tokenB, salesOrderBodyB, "shared-key"))
            .andExpect(status().isCreated())
            .andReturn());

        assertNotEquals(idA, idB);
        assertEquals(2, salesOrderRepository.count());
    }

    @Test
    void blankOrOverLongKeysShouldBeRejected() throws Exception {
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.error").value("ERR_400"));
        mockMvc.perform(create("/api/v1/sales-orders", tokenA, salesOrderBodyA, "k".repeat(101)))
            .andExpect(status().isBadRequest());

        assertEquals(0, salesOrderRepository.count());
    }

    private MockHttpServletRequestBuilder create(String path, String token, String body, String idempotencyKey) {
        MockHttpServletRequestBuilder request = post(path)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
        return idempotencyKey != null ? request.header(HEADER, idempotencyKey) : request;
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private Product product(Organization organization) {
        return productRepository.save(new Product(organization, "W-001", "Widget", null, "Widgets",
            new BigDecimal("10.00"), new BigDecimal("6.00"), 50, 10));
    }
}
