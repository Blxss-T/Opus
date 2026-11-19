import { Link } from "react-router-dom";
import { EmptyState } from "../../shared/components/EmptyState";
import type { SupplierResponse } from "../types";

export function SupplierTable({ suppliers }: { suppliers: SupplierResponse[] }) {
  if (suppliers.length === 0) {
    return <EmptyState message="No suppliers found." />;
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Contact person</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {suppliers.map((supplier) => (
          <tr key={supplier.id}>
            <td>
              <Link to={`/suppliers/${supplier.id}`}>{supplier.name}</Link>
            </td>
            <td>{supplier.contactPerson ?? "—"}</td>
            <td>{supplier.email ?? "—"}</td>
            <td>{supplier.phone ?? "—"}</td>
            <td>{supplier.active ? "Active" : "Inactive"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
