import type { Role } from "../auth/types";

export interface NavItem {
  label: string;
  path: string;
  requiredRoles: Role[];
  enabled: boolean;
}

export const navItems: NavItem[] = [
  { label: "Dashboard", path: "/dashboard", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: true },
  { label: "Products", path: "/products", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "Customers", path: "/customers", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "Suppliers", path: "/suppliers", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "Purchase orders", path: "/purchasing", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "Sales orders", path: "/sales", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "Employees", path: "/employees", requiredRoles: ["ORG_ADMIN", "MANAGER"], enabled: false },
  { label: "Users", path: "/users", requiredRoles: ["ORG_ADMIN", "MANAGER"], enabled: false },
  { label: "Organization", path: "/organizations", requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"], enabled: false },
  { label: "API keys", path: "/api-keys", requiredRoles: ["ORG_ADMIN"], enabled: false },
  { label: "Audit log", path: "/audit", requiredRoles: ["ORG_ADMIN"], enabled: false },
];
