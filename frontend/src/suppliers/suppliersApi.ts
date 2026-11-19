import { apiDelete, apiGet, apiPost, apiPut } from "../api/client";
import type { Page } from "../api/types";
import type { CreateSupplierRequest, SupplierListParams, SupplierResponse, UpdateSupplierRequest } from "./types";

export function createSupplier(body: CreateSupplierRequest): Promise<SupplierResponse> {
  return apiPost<SupplierResponse>("/suppliers", body);
}

export function listSuppliers(params: SupplierListParams): Promise<Page<SupplierResponse>> {
  return apiGet<Page<SupplierResponse>>("/suppliers", { ...params });
}

export function getSupplier(id: string): Promise<SupplierResponse> {
  return apiGet<SupplierResponse>(`/suppliers/${id}`);
}

export function updateSupplier(id: string, body: UpdateSupplierRequest): Promise<SupplierResponse> {
  return apiPut<SupplierResponse>(`/suppliers/${id}`, body);
}

export function deleteSupplier(id: string): Promise<SupplierResponse> {
  return apiDelete<SupplierResponse>(`/suppliers/${id}`);
}
