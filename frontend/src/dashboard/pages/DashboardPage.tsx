import { useQuery } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { useAuth } from "../../auth/useAuth";
import { getDashboardSummary, getLowStockProducts } from "../dashboardApi";
import { SummaryCardGrid } from "../components/SummaryCardGrid";
import { OrdersByStatusCard } from "../components/OrdersByStatusCard";
import { LowStockTable } from "../components/LowStockTable";
import { toErrorMessage } from "../../shared/utils/fieldErrors";

export function DashboardPage() {
  const { hasRole } = useAuth();
  // GET /reports/dashboard is ORG_ADMIN/MANAGER only — EMPLOYEE gets a 403,
  // so this query is disabled entirely for that role rather than firing and failing.
  const canSeeSummary = hasRole(["ORG_ADMIN", "MANAGER"]);

  const summaryQuery = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: getDashboardSummary,
    enabled: canSeeSummary,
  });

  const lowStockQuery = useQuery({
    queryKey: ["dashboard", "low-stock"],
    queryFn: getLowStockProducts,
  });

  return (
    <div className="dashboard-page">
      {canSeeSummary && (
        <section>
          {summaryQuery.isLoading && <Spinner label="Loading summary…" />}
          {summaryQuery.isError && (
            <div className="error-panel">
              <p>{toErrorMessage(summaryQuery.error)}</p>
              <Button onClick={() => summaryQuery.refetch()}>Retry</Button>
            </div>
          )}
          {summaryQuery.data && (
            <>
              <SummaryCardGrid summary={summaryQuery.data} />
              <OrdersByStatusCard statuses={summaryQuery.data.salesOrdersByStatus} />
            </>
          )}
        </section>
      )}

      <section>
        <h2>Low stock</h2>
        {lowStockQuery.isLoading && <Spinner label="Loading low-stock products…" />}
        {lowStockQuery.isError && (
          <div className="error-panel">
            <p>{toErrorMessage(lowStockQuery.error)}</p>
            <Button onClick={() => lowStockQuery.refetch()}>Retry</Button>
          </div>
        )}
        {lowStockQuery.data && <LowStockTable items={lowStockQuery.data} />}
      </section>
    </div>
  );
}
