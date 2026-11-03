package com.opsflow.common.seed;

import com.opsflow.auth.dto.RegisterRequest;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.auth.service.AuthService;
import com.opsflow.customers.dto.CreateCustomerRequest;
import com.opsflow.customers.service.CustomerService;
import com.opsflow.employees.domain.EmploymentStatus;
import com.opsflow.employees.dto.CreateEmployeeRequest;
import com.opsflow.employees.service.EmployeeService;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.dto.CreateProductRequest;
import com.opsflow.inventory.service.ProductService;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.purchasing.dto.CreatePurchaseOrderRequest;
import com.opsflow.purchasing.dto.PurchaseOrderItemRequest;
import com.opsflow.purchasing.dto.UpdatePurchaseOrderStatusRequest;
import com.opsflow.purchasing.service.PurchaseOrderService;
import com.opsflow.sales.domain.SalesOrderStatus;
import com.opsflow.sales.dto.CreateSalesOrderRequest;
import com.opsflow.sales.dto.SalesOrderItemRequest;
import com.opsflow.sales.dto.UpdateSalesOrderStatusRequest;
import com.opsflow.sales.service.SalesOrderService;
import com.opsflow.suppliers.dto.CreateSupplierRequest;
import com.opsflow.suppliers.service.SupplierService;
import com.opsflow.users.domain.Role;
import com.opsflow.users.dto.CreateUserRequest;
import com.opsflow.users.repository.UserRepository;
import com.opsflow.users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Creates a demo organization with users in every role and enough catalog,
 * order and stock data for each endpoint to return something. Enabled only
 * by the "seed" profile, never in prod, and skipped once the demo admin
 * exists. Everything goes through the regular services so the data obeys
 * the same rules as data created through the API.
 */
@Component
@Profile("seed & !prod")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ORGANIZATION_NAME = "Opus Demo Trading";
    public static final String ADMIN_EMAIL = "admin@opus-demo.test";
    public static final String MANAGER_EMAIL = "manager@opus-demo.test";
    public static final String EMPLOYEE_EMAIL = "employee@opus-demo.test";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final String password;

    public DemoDataSeeder(
        AuthService authService,
        UserService userService,
        UserRepository userRepository,
        EmployeeService employeeService,
        CustomerService customerService,
        SupplierService supplierService,
        ProductService productService,
        PurchaseOrderService purchaseOrderService,
        SalesOrderService salesOrderService,
        @Value("${OPUS_SEED_PASSWORD:DemoPass123!}") String password
    ) {
        this.authService = authService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.employeeService = employeeService;
        this.customerService = customerService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.purchaseOrderService = purchaseOrderService;
        this.salesOrderService = salesOrderService;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("Demo data already present; skipping seed");
            return;
        }

        authService.register(new RegisterRequest(ORGANIZATION_NAME, ADMIN_EMAIL, password, "Ada", "Admin"));
        UserPrincipal admin = UserPrincipal.create(userRepository.findByEmail(ADMIN_EMAIL).orElseThrow());

        seedTeam(admin);
        List<UUID> customers = seedCustomers(admin);
        List<UUID> suppliers = seedSuppliers(admin);
        List<UUID> products = seedProducts(admin);
        seedPurchaseOrders(admin, suppliers, products);
        seedSalesOrders(admin, customers, products);

        productService.adjustStock(admin, products.get(4),
            new AdjustStockRequest(-2, MovementType.DAMAGE, "Damaged in warehouse handling"));

        log.info("Demo data seeded: organization '{}', sign in as {} / {} / {}",
            ORGANIZATION_NAME, ADMIN_EMAIL, MANAGER_EMAIL, EMPLOYEE_EMAIL);
    }

    private void seedTeam(UserPrincipal admin) {
        UUID managerId = userService.createUser(admin,
            new CreateUserRequest(MANAGER_EMAIL, password, "Maya", "Manager", Role.MANAGER)).id();
        UUID employeeId = userService.createUser(admin,
            new CreateUserRequest(EMPLOYEE_EMAIL, password, "Eli", "Employee", Role.EMPLOYEE)).id();

        employeeService.create(admin, new CreateEmployeeRequest(
            "Maya", "Manager", MANAGER_EMAIL, "Operations Manager", "Operations",
            EmploymentStatus.ACTIVE, LocalDate.now().minusYears(2), managerId));
        employeeService.create(admin, new CreateEmployeeRequest(
            "Eli", "Employee", EMPLOYEE_EMAIL, "Warehouse Associate", "Warehouse",
            EmploymentStatus.ACTIVE, LocalDate.now().minusMonths(8), employeeId));
        // No linked login: shows an employee record that exists only for HR purposes.
        employeeService.create(admin, new CreateEmployeeRequest(
            "Lena", "Leave", "lena.leave@opus-demo.test", "Sales Representative", "Sales",
            EmploymentStatus.ON_LEAVE, LocalDate.now().minusYears(1), null));
    }

    private List<UUID> seedCustomers(UserPrincipal admin) {
        return List.of(
            customerService.createCustomer(admin, new CreateCustomerRequest(
                "Kigali Fresh Market", "orders@kigalifresh.test", "+250 788 000 101", "KN 4 Ave, Kigali", "Weekly standing order")).id(),
            customerService.createCustomer(admin, new CreateCustomerRequest(
                "Lakeside Hotel", "procurement@lakesidehotel.test", "+250 788 000 102", "Rubavu Waterfront", null)).id(),
            customerService.createCustomer(admin, new CreateCustomerRequest(
                "Hillview Cafe", "hello@hillviewcafe.test", "+250 788 000 103", "KG 9 Ave, Kigali", "Pays on delivery")).id()
        );
    }

    private List<UUID> seedSuppliers(UserPrincipal admin) {
        return List.of(
            supplierService.createSupplier(admin, new CreateSupplierRequest(
                "Great Lakes Wholesale", "Jean Bosco", "sales@greatlakes.test", "+250 788 000 201", "Special Economic Zone, Kigali", "TIN-100200300")).id(),
            supplierService.createSupplier(admin, new CreateSupplierRequest(
                "Highland Packaging", "Aline Uwase", "orders@highlandpack.test", "+250 788 000 202", "Musanze Industrial Park", "TIN-100200301")).id()
        );
    }

    private List<UUID> seedProducts(UserPrincipal admin) {
        return List.of(
            createProduct(admin, "COF-001", "Arabica Coffee Beans 1kg", "Beverages", "18.00", "11.50", 120, 30),
            createProduct(admin, "TEA-001", "Black Tea 500g", "Beverages", "7.50", "4.20", 80, 25),
            createProduct(admin, "SUG-001", "Cane Sugar 2kg", "Groceries", "4.00", "2.60", 200, 50),
            createProduct(admin, "RIC-001", "Long Grain Rice 5kg", "Groceries", "9.90", "6.80", 60, 20),
            // At or below its reorder level so the low-stock report has rows.
            createProduct(admin, "CUP-001", "Paper Cups (pack of 50)", "Packaging", "3.20", "1.90", 12, 15),
            createProduct(admin, "BAG-001", "Kraft Paper Bags (pack of 100)", "Packaging", "6.40", "3.90", 4, 10)
        );
    }

    private UUID createProduct(
        UserPrincipal admin, String sku, String name, String category,
        String unitPrice, String costPrice, int initialStock, int reorderLevel
    ) {
        return productService.createProduct(admin, new CreateProductRequest(
            sku, name, null, category, new BigDecimal(unitPrice), new BigDecimal(costPrice), initialStock, reorderLevel)).id();
    }

    private void seedPurchaseOrders(UserPrincipal admin, List<UUID> suppliers, List<UUID> products) {
        UUID received = purchaseOrderService.createPurchaseOrder(admin, new CreatePurchaseOrderRequest(
            suppliers.get(0), LocalDate.now().minusDays(3), "Monthly restock", List.of(
                new PurchaseOrderItemRequest(products.get(0), 50, new BigDecimal("11.50")),
                new PurchaseOrderItemRequest(products.get(2), 100, new BigDecimal("2.60"))))).id();
        setPurchaseOrderStatus(admin, received, PurchaseOrderStatus.ORDERED);
        setPurchaseOrderStatus(admin, received, PurchaseOrderStatus.RECEIVED);

        UUID ordered = purchaseOrderService.createPurchaseOrder(admin, new CreatePurchaseOrderRequest(
            suppliers.get(1), LocalDate.now().plusDays(5), "Packaging top-up", List.of(
                new PurchaseOrderItemRequest(products.get(4), 40, new BigDecimal("1.90")),
                new PurchaseOrderItemRequest(products.get(5), 30, new BigDecimal("3.90"))))).id();
        setPurchaseOrderStatus(admin, ordered, PurchaseOrderStatus.ORDERED);

        purchaseOrderService.createPurchaseOrder(admin, new CreatePurchaseOrderRequest(
            suppliers.get(0), LocalDate.now().plusDays(14), "Draft for next month", List.of(
                new PurchaseOrderItemRequest(products.get(1), 60, new BigDecimal("4.20")))));
    }

    private void seedSalesOrders(UserPrincipal admin, List<UUID> customers, List<UUID> products) {
        UUID fulfilledOne = createSalesOrder(admin, customers.get(0), "Weekly order", List.of(
            new SalesOrderItemRequest(products.get(0), 20, new BigDecimal("18.00")),
            new SalesOrderItemRequest(products.get(2), 40, new BigDecimal("4.00"))));
        setSalesOrderStatus(admin, fulfilledOne, SalesOrderStatus.CONFIRMED);
        setSalesOrderStatus(admin, fulfilledOne, SalesOrderStatus.FULFILLED);

        UUID fulfilledTwo = createSalesOrder(admin, customers.get(1), "Hotel breakfast supplies", List.of(
            new SalesOrderItemRequest(products.get(1), 15, new BigDecimal("7.50")),
            new SalesOrderItemRequest(products.get(3), 10, new BigDecimal("9.90"))));
        setSalesOrderStatus(admin, fulfilledTwo, SalesOrderStatus.CONFIRMED);
        setSalesOrderStatus(admin, fulfilledTwo, SalesOrderStatus.FULFILLED);

        UUID confirmed = createSalesOrder(admin, customers.get(2), "Awaiting pickup", List.of(
            new SalesOrderItemRequest(products.get(0), 5, new BigDecimal("18.00"))));
        setSalesOrderStatus(admin, confirmed, SalesOrderStatus.CONFIRMED);

        createSalesOrder(admin, customers.get(0), "Draft quote", List.of(
            new SalesOrderItemRequest(products.get(3), 8, new BigDecimal("9.90"))));

        UUID cancelled = createSalesOrder(admin, customers.get(1), "Cancelled by customer", List.of(
            new SalesOrderItemRequest(products.get(2), 25, new BigDecimal("4.00"))));
        setSalesOrderStatus(admin, cancelled, SalesOrderStatus.CANCELLED);
    }

    private UUID createSalesOrder(UserPrincipal admin, UUID customerId, String notes, List<SalesOrderItemRequest> items) {
        return salesOrderService.createSalesOrder(admin,
            new CreateSalesOrderRequest(customerId, LocalDate.now().plusDays(2), notes, items)).id();
    }

    private void setPurchaseOrderStatus(UserPrincipal admin, UUID id, PurchaseOrderStatus status) {
        purchaseOrderService.updateStatus(admin, id, new UpdatePurchaseOrderStatusRequest(status));
    }

    private void setSalesOrderStatus(UserPrincipal admin, UUID id, SalesOrderStatus status) {
        salesOrderService.updateStatus(admin, id, new UpdateSalesOrderStatusRequest(status));
    }
}
