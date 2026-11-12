import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { LoginPage } from "../auth/pages/LoginPage";
import { RegisterPage } from "../auth/pages/RegisterPage";
import { ForgotPasswordPage } from "../auth/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "../auth/pages/ResetPasswordPage";
import { VerifyOtpPage } from "../auth/pages/VerifyOtpPage";
import { DashboardPage } from "../dashboard/pages/DashboardPage";
import { ProductListPage } from "../products/pages/ProductListPage";
import { ProductCreatePage } from "../products/pages/ProductCreatePage";
import { ProductDetailPage } from "../products/pages/ProductDetailPage";
import { CustomerListPage } from "../customers/pages/CustomerListPage";
import { CustomerCreatePage } from "../customers/pages/CustomerCreatePage";
import { CustomerDetailPage } from "../customers/pages/CustomerDetailPage";
import { SupplierListPage } from "../suppliers/pages/SupplierListPage";
import { SupplierCreatePage } from "../suppliers/pages/SupplierCreatePage";
import { SupplierDetailPage } from "../suppliers/pages/SupplierDetailPage";
import { StyleGuidePage } from "../styleguide/StyleGuidePage";
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
      {import.meta.env.DEV && <Route path="/styleguide" element={<StyleGuidePage />} />}

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route element={<RoleGuard allow={["ORG_ADMIN", "MANAGER", "EMPLOYEE"]} />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/products" element={<ProductListPage />} />
            <Route path="/products/:id" element={<ProductDetailPage />} />
            <Route path="/customers" element={<CustomerListPage />} />
            <Route path="/customers/:id" element={<CustomerDetailPage />} />
            <Route path="/suppliers" element={<SupplierListPage />} />
            <Route path="/suppliers/:id" element={<SupplierDetailPage />} />
          </Route>
          <Route element={<RoleGuard allow={["ORG_ADMIN", "MANAGER"]} />}>
            <Route path="/products/new" element={<ProductCreatePage />} />
            <Route path="/customers/new" element={<CustomerCreatePage />} />
            <Route path="/suppliers/new" element={<SupplierCreatePage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<CatchAll />} />
    </Routes>
  );
}
