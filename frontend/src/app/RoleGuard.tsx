import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import type { Role } from "../auth/types";

export function RoleGuard({ allow }: { allow: Role[] }) {
  const { hasRole } = useAuth();

  if (!hasRole(allow)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
