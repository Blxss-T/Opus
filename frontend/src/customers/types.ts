export interface CustomerResponse {
  id: string;
  organizationId: string;
  name: string;
  email: string;
  phone: string | null;
  address: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomerRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
  notes?: string;
}

export type UpdateCustomerRequest = CreateCustomerRequest;

export interface CustomerListParams {
  q?: string;
  page: number;
  size: number;
  sort?: string;
}
