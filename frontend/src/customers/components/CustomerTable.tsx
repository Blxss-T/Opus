import { Link } from "react-router-dom";
import { EmptyState } from "../../shared/components/EmptyState";
import type { CustomerResponse } from "../types";

export function CustomerTable({ customers }: { customers: CustomerResponse[] }) {
  if (customers.length === 0) {
    return <EmptyState message="No customers found." />;
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>Phone</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {customers.map((customer) => (
          <tr key={customer.id}>
            <td>
              <Link to={`/customers/${customer.id}`}>{customer.name}</Link>
            </td>
            <td>{customer.email}</td>
            <td>{customer.phone ?? "—"}</td>
            <td>{customer.active ? "Active" : "Inactive"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
