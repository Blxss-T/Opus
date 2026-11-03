import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { LoginPage } from "../auth/pages/LoginPage";
import { RegisterPage } from "../auth/pages/RegisterPage";
import { ForgotPasswordPage } from "../auth/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "../auth/pages/ResetPasswordPage";
import { VerifyOtpPage } from "../auth/pages/VerifyOtpPage";
import { DashboardPage } from "../dashboard/pages/DashboardPage";
import { AppShell } from "./AppShell";
import { ProtectedRoute } from "./ProtectedRoute";
import { RoleGuard } from "./RoleGuard";

function CatchAll() {
  const { status } = useAuth();
  return <Navigate to={status === "authenticated" ? "/dashboard" : "/login"} replace />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/password/forgot" element={<ForgotPasswordPage />} />
      <Route path="/password/reset" element={<ResetPasswordPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route element={<RoleGuard allow={["ORG_ADMIN", "MANAGER", "EMPLOYEE"]} />}>
            <Route path="/dashboard" element={<DashboardPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<CatchAll />} />
    </Routes>
  );
}
