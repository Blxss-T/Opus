import { EmptyState } from "../../shared/components/EmptyState";
import type { LowStockItem } from "../types";

export function LowStockTable({ items }: { items: LowStockItem[] }) {
  if (items.length === 0) {
    return <EmptyState message="No products are at or below their reorder level." />;
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>SKU</th>
          <th>Name</th>
          <th>Category</th>
          <th>Stock</th>
          <th>Reorder level</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const critical = item.stockQuantity <= item.reorderLevel;
          return (
            <tr key={item.productId} className={critical ? "data-table__row--critical" : undefined}>
              <td>{item.sku}</td>
              <td>{item.name}</td>
              <td>{item.category}</td>
              <td>{item.stockQuantity}</td>
              <td>{item.reorderLevel}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
