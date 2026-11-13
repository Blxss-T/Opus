import type { LucideIcon } from "lucide-react";
import {
  Building2,
  ClipboardList,
  KeyRound,
  LayoutDashboard,
  Package,
  ScrollText,
  ShoppingCart,
  Truck,
  UserCog,
  UserRound,
  Users,
} from "lucide-react";
import type { Role } from "../auth/types";

export interface NavItem {
  label: string;
  path: string;
  requiredRoles: Role[];
  icon: LucideIcon;
}

export const navItems: NavItem[] = [
  {
    label: "Dashboard",
    path: "/dashboard",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: LayoutDashboard,
  },
  {
    label: "Products",
    path: "/products",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: Package,
  },
  {
    label: "Customers",
    path: "/customers",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: Users,
  },
  {
    label: "Suppliers",
    path: "/suppliers",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: Truck,
  },
  {
    label: "Purchase orders",
    path: "/purchasing",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: ClipboardList,
  },
  {
    label: "Sales orders",
    path: "/sales",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: ShoppingCart,
  },
  {
    label: "Employees",
    path: "/employees",
    requiredRoles: ["ORG_ADMIN", "MANAGER"],
    icon: UserRound,
  },
  {
    label: "My profile",
    path: "/employees/me",
    requiredRoles: ["EMPLOYEE"],
    icon: UserRound,
  },
  {
    label: "Users",
    path: "/users",
    requiredRoles: ["ORG_ADMIN", "MANAGER"],
    icon: UserCog,
  },
  {
    label: "Organization",
    path: "/organizations",
    requiredRoles: ["ORG_ADMIN", "MANAGER", "EMPLOYEE"],
    icon: Building2,
  },
  {
    label: "API keys",
    path: "/api-keys",
    requiredRoles: ["ORG_ADMIN"],
    icon: KeyRound,
  },
  {
    label: "Audit log",
    path: "/audit",
    requiredRoles: ["ORG_ADMIN"],
    icon: ScrollText,
  },
];
