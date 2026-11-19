import { Link } from "react-router-dom";
import { EmptyState } from "../../shared/components/EmptyState";
import { formatCurrency } from "../../shared/utils/formatCurrency";
import type { ProductResponse } from "../types";

export function ProductTable({ products }: { products: ProductResponse[] }) {
  if (products.length === 0) {
    return <EmptyState message="No products found." />;
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>SKU</th>
          <th>Name</th>
          <th>Category</th>
          <th>Stock</th>
          <th>Unit price</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {products.map((product) => {
          const critical = product.stockQuantity <= product.reorderLevel;
          return (
            <tr key={product.id} className={critical ? "data-table__row--critical" : undefined}>
              <td>{product.sku}</td>
              <td>
                <Link to={`/products/${product.id}`}>{product.name}</Link>
              </td>
              <td>{product.category}</td>
              <td>{product.stockQuantity}</td>
              <td>{formatCurrency(product.unitPrice)}</td>
              <td>{product.active ? "Active" : "Inactive"}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
