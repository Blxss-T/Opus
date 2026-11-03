import { Card } from "../../shared/components/Card";
import type { OrderStatusCount } from "../types";

export function OrdersByStatusCard({ statuses }: { statuses: OrderStatusCount[] }) {
  return (
    <Card className="orders-by-status">
      <h3>Sales orders by status</h3>
      <ul>
        {statuses.map((entry) => (
          <li key={entry.status}>
            <span className="orders-by-status__label">{entry.status}</span>
            <span className="orders-by-status__count">{entry.orderCount}</span>
          </li>
        ))}
      </ul>
    </Card>
  );
}
