export type SalesOrderStatus = "DRAFT" | "CONFIRMED" | "FULFILLED" | "CANCELLED";

export interface OrderStatusCount {
  status: SalesOrderStatus;
  orderCount: number;
}

export interface DashboardSummaryResponse {
  activeProducts: number;
  lowStockProducts: number;
  activeCustomers: number;
  activeSuppliers: number;
  totalSalesOrders: number;
  openPurchaseOrders: number;
  totalSalesRevenue: number;
  totalPurchaseSpend: number;
  inboundMovementsLast30Days: number;
  outboundMovementsLast30Days: number;
  salesOrdersByStatus: OrderStatusCount[];
}

export interface LowStockItem {
  productId: string;
  sku: string;
  name: string;
  category: string;
  stockQuantity: number;
  reorderLevel: number;
}
