package com.opsflow.reporting.repository;

import com.opsflow.inventory.domain.MovementType;
import com.opsflow.purchasing.domain.PurchaseOrderStatus;
import com.opsflow.reporting.dto.CategorySpendResponse;
import com.opsflow.reporting.dto.CategorySalesResponse;
import com.opsflow.reporting.dto.CustomerRevenueResponse;
import com.opsflow.reporting.dto.DailySalesResponse;
import com.opsflow.reporting.dto.MonthlyTrendResponse;
import com.opsflow.reporting.dto.OrderStatusCountResponse;
import com.opsflow.reporting.dto.SupplierSpendResponse;
import com.opsflow.sales.domain.SalesOrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only reporting queries. Every query is scoped by organization_id taken
 * from the JWT principal, matching the tenant isolation used across modules.
 *
 * All queries are written in JPQL so they run identically on PostgreSQL
 * (production) and H2 in PostgreSQL compatibility mode (tests).
 */
@Repository
public class ReportingRepository {

    private static final int MOVEMENT_WINDOW_DAYS = 30;

    private final EntityManager entityManager;

    public ReportingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countProducts(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(p) FROM Product p " +
                    "WHERE p.organization.id = :organizationId AND p.active = true",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .getSingleResult();
    }

    public long countLowStockProducts(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(p) FROM Product p " +
                    "WHERE p.organization.id = :organizationId " +
                    "AND p.active = true AND p.stockQuantity <= p.reorderLevel",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .getSingleResult();
    }

    public long countCustomers(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(c) FROM Customer c " +
                    "WHERE c.organization.id = :organizationId AND c.active = true",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .getSingleResult();
    }

    public long countSuppliers(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(s) FROM Supplier s " +
                    "WHERE s.organization.id = :organizationId AND s.active = true",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .getSingleResult();
    }

    public long countSalesOrders(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(so) FROM SalesOrder so WHERE so.organization.id = :organizationId",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .getSingleResult();
    }

    public long countOpenPurchaseOrders(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrder po " +
                    "WHERE po.organization.id = :organizationId " +
                    "AND po.status IN :openStatuses",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("openStatuses", List.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.ORDERED))
            .getSingleResult();
    }

    public BigDecimal sumSalesRevenue(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so " +
                    "WHERE so.organization.id = :organizationId " +
                    "AND so.status = :fulfilled",
                BigDecimal.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("fulfilled", SalesOrderStatus.FULFILLED)
            .getSingleResult();
    }

    public BigDecimal sumPurchaseSpend(UUID organizationId) {
        return entityManager.createQuery(
                "SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po " +
                    "WHERE po.organization.id = :organizationId " +
                    "AND po.status = :received",
                BigDecimal.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("received", PurchaseOrderStatus.RECEIVED)
            .getSingleResult();
    }

    public List<OrderStatusCountResponse> countSalesOrdersByStatus(UUID organizationId) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT so.status AS status, COUNT(so) AS orderCount " +
                    "FROM SalesOrder so WHERE so.organization.id = :organizationId " +
                    "GROUP BY so.status",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .getResultList();

        return rows.stream()
            .map(row -> new OrderStatusCountResponse(
                row.get("status", SalesOrderStatus.class),
                row.get("orderCount", Long.class)
            ))
            .toList();
    }

    public List<CustomerRevenueResponse> findTopCustomersByRevenue(UUID organizationId, int limit) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT c.id AS customerId, c.name AS customerName, " +
                    "COUNT(so) AS orderCount, COALESCE(SUM(so.totalAmount), 0) AS totalRevenue " +
                    "FROM SalesOrder so JOIN so.customer c " +
                    "WHERE so.organization.id = :organizationId AND so.status = :fulfilled " +
                    "GROUP BY c.id, c.name " +
                    "ORDER BY COALESCE(SUM(so.totalAmount), 0) DESC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("fulfilled", SalesOrderStatus.FULFILLED)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(row -> new CustomerRevenueResponse(
                row.get("customerId", UUID.class),
                row.get("customerName", String.class),
                row.get("orderCount", Long.class),
                row.get("totalRevenue", BigDecimal.class)
            ))
            .toList();
    }

    public List<SupplierSpendResponse> findTopSuppliersBySpend(UUID organizationId, int limit) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT s.id AS supplierId, s.name AS supplierName, " +
                    "COUNT(po) AS orderCount, COALESCE(SUM(po.totalAmount), 0) AS totalSpend " +
                    "FROM PurchaseOrder po JOIN po.supplier s " +
                    "WHERE po.organization.id = :organizationId AND po.status = :received " +
                    "GROUP BY s.id, s.name " +
                    "ORDER BY COALESCE(SUM(po.totalAmount), 0) DESC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("received", PurchaseOrderStatus.RECEIVED)
            .setMaxResults(limit)
            .getResultList();

        return rows.stream()
            .map(row -> new SupplierSpendResponse(
                row.get("supplierId", UUID.class),
                row.get("supplierName", String.class),
                row.get("orderCount", Long.class),
                row.get("totalSpend", BigDecimal.class)
            ))
            .toList();
    }

    public List<CategorySalesResponse> findSalesByCategory(UUID organizationId) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT soi.product.category AS category, " +
                    "COALESCE(SUM(soi.subtotal), 0) AS totalSales " +
                    "FROM SalesOrderItem soi JOIN soi.salesOrder so " +
                    "WHERE so.organization.id = :organizationId AND so.status = :fulfilled " +
                    "GROUP BY soi.product.category " +
                    "ORDER BY COALESCE(SUM(soi.subtotal), 0) DESC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("fulfilled", SalesOrderStatus.FULFILLED)
            .getResultList();

        return rows.stream()
            .map(row -> new CategorySalesResponse(
                row.get("category", String.class),
                row.get("totalSales", BigDecimal.class)
            ))
            .toList();
    }

    public List<CategorySpendResponse> findPurchasesByCategory(UUID organizationId) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT poi.product.category AS category, " +
                    "COALESCE(SUM(poi.subtotal), 0) AS totalSpend " +
                    "FROM PurchaseOrderItem poi JOIN poi.purchaseOrder po " +
                    "WHERE po.organization.id = :organizationId AND po.status = :received " +
                    "GROUP BY poi.product.category " +
                    "ORDER BY COALESCE(SUM(poi.subtotal), 0) DESC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("received", PurchaseOrderStatus.RECEIVED)
            .getResultList();

        return rows.stream()
            .map(row -> new CategorySpendResponse(
                row.get("category", String.class),
                row.get("totalSpend", BigDecimal.class)
            ))
            .toList();
    }

    public List<DailySalesResponse> findDailySalesLast30Days(UUID organizationId) {
        Instant since = Instant.now().minusSeconds((long) MOVEMENT_WINDOW_DAYS * 24 * 60 * 60);

        List<Tuple> rows = entityManager.createQuery(
                "SELECT CAST(so.createdAt AS LocalDate) AS saleDate, " +
                    "COALESCE(SUM(so.totalAmount), 0) AS totalSales " +
                    "FROM SalesOrder so " +
                    "WHERE so.organization.id = :organizationId " +
                    "AND so.status = :fulfilled AND so.createdAt >= :since " +
                    "GROUP BY CAST(so.createdAt AS LocalDate) " +
                    "ORDER BY CAST(so.createdAt AS LocalDate) ASC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("fulfilled", SalesOrderStatus.FULFILLED)
            .setParameter("since", since)
            .getResultList();

        return rows.stream()
            .map(row -> new DailySalesResponse(
                row.get("saleDate", LocalDate.class),
                row.get("totalSales", BigDecimal.class)
            ))
            .toList();
    }

    public List<MonthlyTrendResponse> findMonthlySalesTrend(UUID organizationId) {
        List<Tuple> rows = entityManager.createQuery(
                "SELECT YEAR(so.createdAt) AS yr, MONTH(so.createdAt) AS mo, " +
                    "COALESCE(SUM(so.totalAmount), 0) AS totalSales " +
                    "FROM SalesOrder so " +
                    "WHERE so.organization.id = :organizationId AND so.status = :fulfilled " +
                    "GROUP BY YEAR(so.createdAt), MONTH(so.createdAt) " +
                    "ORDER BY YEAR(so.createdAt) ASC, MONTH(so.createdAt) ASC",
                Tuple.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("fulfilled", SalesOrderStatus.FULFILLED)
            .getResultList();

        return rows.stream()
            .map(row -> new MonthlyTrendResponse(
                LocalDate.of(row.get("yr", Integer.class), row.get("mo", Integer.class), 1),
                row.get("totalSales", BigDecimal.class)
            ))
            .toList();
    }

    public long countMovementsLast30Days(UUID organizationId, MovementType movementType) {
        Instant since = Instant.now().minusSeconds((long) MOVEMENT_WINDOW_DAYS * 24 * 60 * 60);

        return entityManager.createQuery(
                "SELECT COUNT(im) FROM InventoryMovement im " +
                    "WHERE im.organization.id = :organizationId " +
                    "AND im.movementType = :movementType AND im.createdAt >= :since",
                Long.class
            )
            .setParameter("organizationId", organizationId)
            .setParameter("movementType", movementType)
            .setParameter("since", since)
            .getSingleResult();
    }
}
