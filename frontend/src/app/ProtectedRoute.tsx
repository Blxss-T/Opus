import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { Spinner } from "../shared/components/Spinner";

export function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === "initializing") {
    return <Spinner label="Loading…" />;
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
