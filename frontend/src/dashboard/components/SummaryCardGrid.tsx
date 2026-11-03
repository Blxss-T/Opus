import { Card } from "../../shared/components/Card";
import { formatCurrency } from "../../shared/utils/formatCurrency";
import type { DashboardSummaryResponse } from "../types";

export function SummaryCardGrid({ summary }: { summary: DashboardSummaryResponse }) {
  const cards: { label: string; value: string }[] = [
    { label: "Active products", value: String(summary.activeProducts) },
    { label: "Low stock products", value: String(summary.lowStockProducts) },
    { label: "Active customers", value: String(summary.activeCustomers) },
    { label: "Active suppliers", value: String(summary.activeSuppliers) },
    { label: "Total sales orders", value: String(summary.totalSalesOrders) },
    { label: "Open purchase orders", value: String(summary.openPurchaseOrders) },
    { label: "Total sales revenue", value: formatCurrency(summary.totalSalesRevenue) },
    { label: "Total purchase spend", value: formatCurrency(summary.totalPurchaseSpend) },
    { label: "Inbound movements (30d)", value: String(summary.inboundMovementsLast30Days) },
    { label: "Outbound movements (30d)", value: String(summary.outboundMovementsLast30Days) },
  ];

  return (
    <div className="summary-grid">
      {cards.map((card) => (
        <Card key={card.label} className="summary-card">
          <p className="summary-card__label">{card.label}</p>
          <p className="summary-card__value">{card.value}</p>
        </Card>
      ))}
    </div>
  );
}
