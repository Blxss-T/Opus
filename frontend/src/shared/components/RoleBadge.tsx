import type { Role } from "../../auth/types";
import { StatusPill, type StatusTone } from "./StatusPill";

const roleLabel: Record<Role, string> = {
  ORG_ADMIN: "Admin",
  MANAGER: "Manager",
  EMPLOYEE: "Employee",
  SUPER_ADMIN: "Super admin",
};

const roleTone: Record<Role, StatusTone> = {
  ORG_ADMIN: "ordered",
  MANAGER: "neutral",
  EMPLOYEE: "draft",
  SUPER_ADMIN: "ordered",
};

export function RoleBadge({ role }: { role: Role }) {
  return <StatusPill tone={roleTone[role]}>{roleLabel[role]}</StatusPill>;
}
