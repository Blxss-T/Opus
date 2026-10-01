package com.opsflow.reporting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.auth.security.JwtService;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.purchasing.dto.CreatePurchaseOrderRequest;
import com.opsflow.purchasing.dto.PurchaseOrderItemRequest;
import com.opsflow.purchasing.dto.UpdatePurchaseOrderStatusRequest;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.purchasing.repository.PurchaseOrderRepository;
import com.opsflow.sales.dto.CreateSalesOrderRequest;
import com.opsflow.sales.dto.SalesOrderItemRequest;
import com.opsflow.sales.dto.UpdateSalesOrderStatusRequest;
import com.opsflow.sales.domain.SalesOrderStatus;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

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

    private Organization orgA;
    private Organization orgB;

    private User adminA;
    private User adminB;

    private String adminAToken;
    private String adminBToken;

    private Customer customerA;
    private Supplier supplierA;
    private Product productA;
    private Product lowStockProduct;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();

        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        adminA = userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN));
        adminB = userRepository.save(new User(orgB, "admin@beta.com", "hash", "Admin", "Beta", Role.ORG_ADMIN));

        adminAToken = jwtService.generateToken(adminA);
        adminBToken = jwtService.generateToken(adminB);

        customerA = customerRepository.save(new Customer(orgA, "Customer Alpha", "alpha@customer.com", null, null, null));
        supplierA = supplierRepository.save(new Supplier(orgA, "Supplier Alpha", null, null, null, null, null));

        productA = productRepository.save(new Product(
            orgA,
            "SKU-REP-1",
            "Bulk Cable",
            "Cat6 cable",
            "Cables",
            new BigDecimal("150.00"),
            new BigDecimal("80.00"),
            100,
            5
        ));

        lowStockProduct = productRepository.save(new Product(
            orgA,
            "SKU-REP-LOW",
            "Low Stock Item",
            null,
            "Cables",
            new BigDecimal("40.00"),
            new BigDecimal("20.00"),
            3,
            10
        ));
    }

    private MvcResult createAndFulfillSalesOrder(int quantity, String unitPrice) throws Exception {
        SalesOrderItemRequest item = new SalesOrderItemRequest(productA.getId(), quantity, new BigDecimal(unitPrice));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult created = mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String soId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateSalesOrderStatusRequest(SalesOrderStatus.CONFIRMED))))
            .andExpect(status().isOk());

        return mockMvc.perform(patch("/api/v1/sales-orders/" + soId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateSalesOrderStatusRequest(SalesOrderStatus.FULFILLED))))
            .andExpect(status().isOk())
            .andReturn();
    }

    private void createAndReceivePurchaseOrder(int quantity, String unitCost) throws Exception {
        PurchaseOrderItemRequest item = new PurchaseOrderItemRequest(productA.getId(), quantity, new BigDecimal(unitCost));
        CreatePurchaseOrderRequest poRequest = new CreatePurchaseOrderRequest(
            supplierA.getId(),
            null,
            null,
            List.of(item)
        );

        MvcResult created = mockMvc.perform(post("/api/v1/purchase-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(poRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String poId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/purchase-orders/" + poId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.ORDERED))))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/purchase-orders/" + poId + "/status")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdatePurchaseOrderStatusRequest(PurchaseOrderStatus.RECEIVED))))
            .andExpect(status().isOk());
    }

    @Test
    void dashboardShouldAggregateMetricsAfterFulfillingSOAndReceivingPO() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");
        createAndReceivePurchaseOrder(20, "80.00");

        mockMvc.perform(get("/api/v1/reports/dashboard")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.activeProducts").value(2))
            .andExpect(jsonPath("$.data.lowStockProducts").value(1))
            .andExpect(jsonPath("$.data.activeCustomers").value(1))
            .andExpect(jsonPath("$.data.activeSuppliers").value(1))
            .andExpect(jsonPath("$.data.totalSalesOrders").value(1))
            .andExpect(jsonPath("$.data.openPurchaseOrders").value(0))
            .andExpect(jsonPath("$.data.totalSalesRevenue").value(1500.0))
            .andExpect(jsonPath("$.data.totalPurchaseSpend").value(1600.0))
            .andExpect(jsonPath("$.data.inboundMovementsLast30Days").value(1))
            .andExpect(jsonPath("$.data.outboundMovementsLast30Days").value(1))
            .andExpect(jsonPath("$.data.salesOrdersByStatus[0].status").value("FULFILLED"))
            .andExpect(jsonPath("$.data.salesOrdersByStatus[0].orderCount").value(1));
    }

    @Test
    void lowStockEndpointShouldListProductsAtOrBelowReorderLevel() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");

        mockMvc.perform(get("/api/v1/reports/dashboard/low-stock")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].sku").value("SKU-REP-LOW"))
            .andExpect(jsonPath("$.data[0].stockQuantity").value(3))
            .andExpect(jsonPath("$.data[0].reorderLevel").value(10));
    }

    @Test
    void topCustomersShouldRankByFulfilledRevenue() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");

        mockMvc.perform(get("/api/v1/reports/sales/top-customers")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].customerName").value("Customer Alpha"))
            .andExpect(jsonPath("$.data[0].orderCount").value(1))
            .andExpect(jsonPath("$.data[0].totalRevenue").value(1500.0));
    }

    @Test
    void topSuppliersShouldRankByReceivedSpend() throws Exception {
        createAndReceivePurchaseOrder(20, "80.00");

        mockMvc.perform(get("/api/v1/reports/purchases/top-suppliers")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].supplierName").value("Supplier Alpha"))
            .andExpect(jsonPath("$.data[0].totalSpend").value(1600.0));
    }

    @Test
    void salesByCategoryShouldAggregateFulfilledRevenuePerCategory() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");

        mockMvc.perform(get("/api/v1/reports/sales/by-category")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].category").value("Cables"))
            .andExpect(jsonPath("$.data[0].totalSales").value(1500.0));
    }

    @Test
    void dailyAndMonthlySalesShouldOnlyIncludeFulfilledOrders() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");

        mockMvc.perform(get("/api/v1/reports/sales/daily")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].totalSales").value(1500.0));

        mockMvc.perform(get("/api/v1/reports/sales/monthly-trend")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].totalSales").value(1500.0));
    }

    @Test
    void tenantIsolationShouldKeepReportsScopedToOwnOrganization() throws Exception {
        createAndFulfillSalesOrder(10, "150.00");
        createAndReceivePurchaseOrder(20, "80.00");

        // Org B has no data; every report it sees must be zero/empty
        MvcResult result = mockMvc.perform(get("/api/v1/reports/dashboard")
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isOk())
            .andReturn();

        var dashboard = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertEquals(0, dashboard.path("totalSalesOrders").asLong());
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(new java.math.BigDecimal(dashboard.path("totalSalesRevenue").asText())));
        assertTrue(dashboard.path("salesOrdersByStatus").isEmpty());

        mockMvc.perform(get("/api/v1/reports/sales/top-customers")
                .header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void employeeShouldAccessReportsButNotWriteEndpoints() throws Exception {
        User employee = userRepository.save(new User(orgA, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE));
        String employeeToken = jwtService.generateToken(employee);

        mockMvc.perform(get("/api/v1/reports/dashboard/low-stock")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/dashboard")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reportsShouldShowZeroTransactionalMetricsBeforeAnyOrders() throws Exception {
        // setUp seeds reference data (2 products, 1 customer, 1 supplier) but no orders
        mockMvc.perform(get("/api/v1/reports/dashboard")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.activeProducts").value(2))
            .andExpect(jsonPath("$.data.lowStockProducts").value(1))
            .andExpect(jsonPath("$.data.totalSalesOrders").value(0))
            .andExpect(jsonPath("$.data.totalSalesRevenue").value(0.0))
            .andExpect(jsonPath("$.data.totalPurchaseSpend").value(0.0))
            .andExpect(jsonPath("$.data.salesOrdersByStatus").isEmpty());

        mockMvc.perform(get("/api/v1/reports/sales/by-category")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void draftOrdersShouldNotCountAsRevenueOrSpend() throws Exception {
        // Create a DRAFT sales order (never confirmed/fulfilled)
        SalesOrderItemRequest soItem = new SalesOrderItemRequest(productA.getId(), 10, new BigDecimal("150.00"));
        CreateSalesOrderRequest soRequest = new CreateSalesOrderRequest(
            customerA.getId(),
            null,
            null,
            List.of(soItem)
        );
        mockMvc.perform(post("/api/v1/sales-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soRequest)))
            .andExpect(status().isCreated());

        // Create a DRAFT purchase order (never ordered/received)
        PurchaseOrderItemRequest poItem = new PurchaseOrderItemRequest(productA.getId(), 20, new BigDecimal("80.00"));
        CreatePurchaseOrderRequest poRequest = new CreatePurchaseOrderRequest(
            supplierA.getId(),
            null,
            null,
            List.of(poItem)
        );
        mockMvc.perform(post("/api/v1/purchase-orders")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(poRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reports/dashboard")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalSalesRevenue").value(0.0))
            .andExpect(jsonPath("$.data.totalPurchaseSpend").value(0.0))
            .andExpect(jsonPath("$.data.openPurchaseOrders").value(1));

        // Ensure repositories still hold the draft records for this org
        assertEquals(1, salesOrderRepository.count());
        assertEquals(1, purchaseOrderRepository.count());
    }
}
