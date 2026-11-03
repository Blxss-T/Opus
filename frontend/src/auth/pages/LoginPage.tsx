import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { PasswordField } from "../components/PasswordField";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import { useAuth } from "../useAuth";

interface LocationState {
  from?: { pathname: string };
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const sessionExpired = new URLSearchParams(location.search).get("sessionExpired") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ email, password });
      const state = location.state as LocationState | null;
      navigate(state?.from?.pathname ?? "/dashboard", { replace: true });
    } catch {
      // Deliberately generic: don't reveal whether the email or password was wrong.
      setError("Invalid email or password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Sign in">
      {sessionExpired && <p className="auth-banner">Your session expired. Please sign in again.</p>}
      <form onSubmit={handleSubmit}>
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <PasswordField
          label="Password"
          name="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={setPassword}
        />
        {error && <p className="form-field__error">{error}</p>}
        <Button type="submit" loading={submitting}>
          Sign in
        </Button>
      </form>
      <p>
        <Link to="/password/forgot">Forgot your password?</Link>
      </p>
      <p>
        Don&apos;t have an account? <Link to="/register">Register your organization</Link>
      </p>
    </AuthLayout>
  );
}
