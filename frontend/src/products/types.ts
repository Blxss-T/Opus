export type MovementType = "INBOUND" | "OUTBOUND" | "ADJUSTMENT" | "RETURN" | "DAMAGE";

export interface ProductResponse {
  id: string;
  organizationId: string;
  sku: string;
  name: string;
  description: string | null;
  category: string;
  unitPrice: number;
  costPrice: number;
  stockQuantity: number;
  reorderLevel: number;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  description?: string;
  category: string;
  unitPrice: number;
  costPrice: number;
  initialStock: number;
  reorderLevel: number;
}

export interface UpdateProductRequest {
  name: string;
  description?: string;
  category: string;
  unitPrice: number;
  costPrice: number;
  reorderLevel: number;
}

export interface AdjustStockRequest {
  quantityDelta: number;
  movementType: MovementType;
  reason: string;
}

export interface InventoryMovementResponse {
  id: string;
  productId: string;
  quantityDelta: number;
  previousQuantity: number;
  newQuantity: number;
  movementType: MovementType;
  reason: string;
  performedBy: string;
  performedByEmail: string;
  createdAt: string;
}

export interface ProductListParams {
  q?: string;
  category?: string;
  page: number;
  size: number;
  sort?: string;
}

export interface MovementListParams {
  type?: MovementType;
  page: number;
  size: number;
  sort?: string;
}
