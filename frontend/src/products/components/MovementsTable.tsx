import { EmptyState } from "../../shared/components/EmptyState";
import { formatDate } from "../../shared/utils/formatDate";
import type { InventoryMovementResponse, MovementType } from "../types";

const movementTypes: MovementType[] = ["INBOUND", "OUTBOUND", "ADJUSTMENT", "RETURN", "DAMAGE"];

interface MovementsTableProps {
  movements: InventoryMovementResponse[];
  typeFilter: MovementType | undefined;
  onTypeFilterChange: (type: MovementType | undefined) => void;
}

export function MovementsTable({ movements, typeFilter, onTypeFilterChange }: MovementsTableProps) {
  return (
    <div>
      <div className="form-field">
        <label htmlFor="movementTypeFilter">Filter by type</label>
        <select
          id="movementTypeFilter"
          className="form-field__input"
          value={typeFilter ?? ""}
          onChange={(e) => onTypeFilterChange((e.target.value || undefined) as MovementType | undefined)}
        >
          <option value="">All</option>
          {movementTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </div>

      {movements.length === 0 ? (
        <EmptyState message="No stock movements yet." />
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Qty delta</th>
              <th>Previous → New</th>
              <th>Reason</th>
              <th>Performed by</th>
            </tr>
          </thead>
          <tbody>
            {movements.map((movement) => (
              <tr key={movement.id}>
                <td>{formatDate(movement.createdAt)}</td>
                <td>{movement.movementType}</td>
                <td>{movement.quantityDelta}</td>
                <td>
                  {movement.previousQuantity} → {movement.newQuantity}
                </td>
                <td>{movement.reason}</td>
                <td>{movement.performedByEmail}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
