import { useState, type FormEvent } from "react";
import { changePassword } from "../auth/authApi";
import { Button } from "../shared/components/Button";
import { Modal } from "../shared/components/Modal";
import { useToast } from "../shared/components/Toast";
import { PasswordField } from "../auth/components/PasswordField";
import { toErrorMessage, toFieldErrorMap } from "../shared/utils/fieldErrors";

export function ChangePasswordModal({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { show } = useToast();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  function resetForm() {
    setCurrentPassword("");
    setNewPassword("");
    setFieldErrors({});
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFieldErrors({});
    setSubmitting(true);
    try {
      await changePassword({ currentPassword, newPassword });
      show("Password updated.", "success");
      resetForm();
      onOpenChange(false);
    } catch (error) {
      const fields = toFieldErrorMap(error);
      if (Object.keys(fields).length > 0) {
        setFieldErrors(fields);
      } else {
        show(toErrorMessage(error), "error");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          resetForm();
        }
        onOpenChange(next);
      }}
      title="Change password"
      description="Enter your current password and a new password of at least 8 characters."
    >
      <form onSubmit={handleSubmit}>
        <PasswordField
          label="Current password"
          name="currentPassword"
          autoComplete="current-password"
          required
          value={currentPassword}
          error={fieldErrors.currentPassword}
          onChange={setCurrentPassword}
        />
        <PasswordField
          label="New password"
          name="newPassword"
          autoComplete="new-password"
          required
          minLength={8}
          value={newPassword}
          error={fieldErrors.newPassword}
          onChange={setNewPassword}
        />
        <div className="mt-2 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="submit" loading={submitting}>
            Update password
          </Button>
        </div>
      </form>
    </Modal>
  );
}
