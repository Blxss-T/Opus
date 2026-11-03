import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { PasswordField } from "../components/PasswordField";
import { FormField } from "../../shared/components/FormField";
import { Button } from "../../shared/components/Button";
import { useAuth } from "../useAuth";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { RegisterRequest } from "../types";

const initialForm: RegisterRequest = {
  organizationName: "",
  email: "",
  password: "",
  firstName: "",
  lastName: "",
};

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState<RegisterRequest>(initialForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function update<K extends keyof RegisterRequest>(key: K, value: RegisterRequest[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      await register(form);
      navigate("/dashboard", { replace: true });
    } catch (error) {
      const fields = toFieldErrorMap(error);
      if (Object.keys(fields).length > 0) {
        setFieldErrors(fields);
      } else {
        setFormError(toErrorMessage(error));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Register your organization">
      <form onSubmit={handleSubmit}>
        <FormField
          label="Organization name"
          name="organizationName"
          required
          value={form.organizationName}
          error={fieldErrors.organizationName}
          onChange={(e) => update("organizationName", e.target.value)}
        />
        <FormField
          label="First name"
          name="firstName"
          required
          value={form.firstName}
          error={fieldErrors.firstName}
          onChange={(e) => update("firstName", e.target.value)}
        />
        <FormField
          label="Last name"
          name="lastName"
          required
          value={form.lastName}
          error={fieldErrors.lastName}
          onChange={(e) => update("lastName", e.target.value)}
        />
        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={form.email}
          error={fieldErrors.email}
          onChange={(e) => update("email", e.target.value)}
        />
        <PasswordField
          label="Password"
          name="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={form.password}
          error={fieldErrors.password}
          onChange={(value) => update("password", value)}
        />
        {formError && <p className="form-field__error">{formError}</p>}
        <Button type="submit" loading={submitting}>
          Create organization
        </Button>
      </form>
      <p>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthLayout>
  );
}
