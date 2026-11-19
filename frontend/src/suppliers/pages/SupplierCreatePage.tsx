import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createSupplier } from "../suppliersApi";
import { SupplierForm } from "../components/SupplierForm";
import { useToast } from "../../shared/components/Toast";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { CreateSupplierRequest } from "../types";

const initialForm: CreateSupplierRequest = {
  name: "",
  contactPerson: "",
  email: "",
  phone: "",
  address: "",
  taxNumber: "",
};

export function SupplierCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createSupplier,
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ["suppliers", "list"] });
      toast.show("Supplier created", "success");
      navigate(`/suppliers/${created.id}`, { replace: true });
    },
    onError: (error) => {
      const fields = toFieldErrorMap(error);
      if (Object.keys(fields).length > 0) {
        setFieldErrors(fields);
      } else {
        setFormError(toErrorMessage(error));
      }
    },
  });

  function handleSubmit(values: CreateSupplierRequest) {
    setFieldErrors({});
    setFormError(null);
    mutation.mutate(values);
  }

  return (
    <div>
      <SupplierForm
        initial={initialForm}
        submitLabel="Create supplier"
        submitting={mutation.isPending}
        fieldErrors={fieldErrors}
        formError={formError}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
