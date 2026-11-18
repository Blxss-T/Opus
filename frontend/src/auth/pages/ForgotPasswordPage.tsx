import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import * as authApi from "../authApi";

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await authApi.forgotPassword({ email });
    } catch {
      // Intentionally ignored: the backend's response never reveals whether
      // the account exists, so the frontend shows the same next step either way.
    } finally {
      setSubmitting(false);
      navigate(`/password/reset?email=${encodeURIComponent(email)}`);
    }
  }

  return (
    <AuthLayout
      title="Forgot your password?"
      subtitle="Enter the email for your account. We will send a reset code either way."
    >
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
        <Button type="submit" variant="inverse" className="w-full" loading={submitting}>
          Send reset code
        </Button>
      </form>
      <p className="mt-4 text-small">
        <Link to="/login">Back to sign in</Link>
      </p>
    </AuthLayout>
  );
}
