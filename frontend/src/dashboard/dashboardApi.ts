import { apiGet } from "../api/client";
import type { DashboardSummaryResponse, LowStockItem } from "./types";

export function getDashboardSummary(): Promise<DashboardSummaryResponse> {
  return apiGet<DashboardSummaryResponse>("/reports/dashboard");
}

export function getLowStockProducts(): Promise<LowStockItem[]> {
  return apiGet<LowStockItem[]>("/reports/dashboard/low-stock");
}
