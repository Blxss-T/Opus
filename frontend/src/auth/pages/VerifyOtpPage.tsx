import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/AuthLayout";
import { OtpInput } from "../components/OtpInput";
import { Button } from "../../shared/components/Button";
import * as authApi from "../authApi";
import { toErrorMessage } from "../../shared/utils/fieldErrors";
import { useAuth } from "../useAuth";
import type { OtpType } from "../types";

// Generic email-OTP verification screen. Phase 1 only reaches this for
// EMAIL_VERIFICATION after registration; LOGIN_2FA is not wired into the
// login flow yet even though the component supports that `type` value.
export function VerifyOtpPage({ type = "EMAIL_VERIFICATION" as OtpType }: { type?: OtpType }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const email = user?.email ?? "";

  async function handleResend() {
    setResendMessage(null);
    try {
      await authApi.sendOtp({ email, type });
      setResendMessage("A new code has been sent.");
    } catch (err) {
      setResendMessage(toErrorMessage(err));
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.verifyOtp({ email, code, type });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(toErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout title="Verify your email" subtitle={`We sent a verification code to ${email}.`}>
      <form onSubmit={handleSubmit}>
        <OtpInput value={code} onChange={setCode} error={error ?? undefined} />
        <Button type="submit" variant="inverse" className="w-full" loading={submitting}>
          Verify
        </Button>
      </form>
      <button
        type="button"
        className="mt-3 text-small font-medium text-primary-600"
        onClick={handleResend}
      >
        Resend code
      </button>
      {resendMessage && <p className="mt-2 text-small text-muted">{resendMessage}</p>}
    </AuthLayout>
  );
}
