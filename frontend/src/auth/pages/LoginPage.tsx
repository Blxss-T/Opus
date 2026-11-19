import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { GoogleSignInButton } from "../components/GoogleSignInButton";
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
    <AuthLayout title="Sign in" subtitle="Enter your email and password to continue.">
      {sessionExpired && (
        <p className="mb-4 rounded-md bg-primary-600 px-3 py-2 text-small text-white">
          Your session expired. Please sign in again.
        </p>
      )}
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
        {error && (
          <p className="mb-3 text-caption text-danger" role="alert">
            {error}
          </p>
        )}
        <Button type="submit" variant="inverse" className="w-full" loading={submitting}>
          Sign in
        </Button>
      </form>
      <GoogleSignInButton
        disabled={submitting}
        onSignedIn={() => {
          const state = location.state as LocationState | null;
          navigate(state?.from?.pathname ?? "/dashboard", { replace: true });
        }}
      />
      <p className="mt-4 text-small">
        <Link to="/password/forgot">Forgot your password?</Link>
      </p>
      <p className="text-small text-muted">
        Don&apos;t have an account? <Link to="/register">Register your organization</Link>
      </p>
    </AuthLayout>
  );
}
