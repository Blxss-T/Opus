export interface SupplierResponse {
  id: string;
  organizationId: string;
  name: string;
  contactPerson: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  taxNumber: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  taxNumber?: string;
}

export type UpdateSupplierRequest = CreateSupplierRequest;

export interface SupplierListParams {
  q?: string;
  page: number;
  size: number;
  sort?: string;
}
