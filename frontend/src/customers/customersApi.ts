import { apiDelete, apiGet, apiPost, apiPut } from "../api/client";
import type { Page } from "../api/types";
import type { CreateCustomerRequest, CustomerListParams, CustomerResponse, UpdateCustomerRequest } from "./types";

export function createCustomer(body: CreateCustomerRequest): Promise<CustomerResponse> {
  return apiPost<CustomerResponse>("/customers", body);
}

export function listCustomers(params: CustomerListParams): Promise<Page<CustomerResponse>> {
  return apiGet<Page<CustomerResponse>>("/customers", { ...params });
}

export function getCustomer(id: string): Promise<CustomerResponse> {
  return apiGet<CustomerResponse>(`/customers/${id}`);
}

export function updateCustomer(id: string, body: UpdateCustomerRequest): Promise<CustomerResponse> {
  return apiPut<CustomerResponse>(`/customers/${id}`, body);
}

export function deleteCustomer(id: string): Promise<CustomerResponse> {
  return apiDelete<CustomerResponse>(`/customers/${id}`);
}
