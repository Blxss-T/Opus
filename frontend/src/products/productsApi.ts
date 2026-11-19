import { apiDelete, apiGet, apiPost, apiPut } from "../api/client";
import type { Page } from "../api/types";
import type {
  AdjustStockRequest,
  CreateProductRequest,
  InventoryMovementResponse,
  MovementListParams,
  ProductListParams,
  ProductResponse,
  UpdateProductRequest,
} from "./types";

export function createProduct(body: CreateProductRequest): Promise<ProductResponse> {
  return apiPost<ProductResponse>("/products", body);
}

export function listProducts(params: ProductListParams): Promise<Page<ProductResponse>> {
  return apiGet<Page<ProductResponse>>("/products", { ...params });
}

export function getProduct(id: string): Promise<ProductResponse> {
  return apiGet<ProductResponse>(`/products/${id}`);
}

export function updateProduct(id: string, body: UpdateProductRequest): Promise<ProductResponse> {
  return apiPut<ProductResponse>(`/products/${id}`, body);
}

export function deleteProduct(id: string): Promise<ProductResponse> {
  return apiDelete<ProductResponse>(`/products/${id}`);
}

export function adjustStock(id: string, body: AdjustStockRequest): Promise<ProductResponse> {
  return apiPost<ProductResponse>(`/products/${id}/adjust-stock`, body);
}

export function getMovements(id: string, params: MovementListParams): Promise<Page<InventoryMovementResponse>> {
  return apiGet<Page<InventoryMovementResponse>>(`/products/${id}/movements`, { ...params });
}
