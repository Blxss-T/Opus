import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createCustomer } from "../customersApi";
import { CustomerForm } from "../components/CustomerForm";
import { useToast } from "../../shared/components/Toast";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { CreateCustomerRequest } from "../types";

const initialForm: CreateCustomerRequest = { name: "", email: "", phone: "", address: "", notes: "" };

export function CustomerCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createCustomer,
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ["customers", "list"] });
      toast.show("Customer created", "success");
      navigate(`/customers/${created.id}`, { replace: true });
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

  function handleSubmit(values: CreateCustomerRequest) {
    setFieldErrors({});
    setFormError(null);
    mutation.mutate(values);
  }

  return (
    <div>
      <CustomerForm
        initial={initialForm}
        submitLabel="Create customer"
        submitting={mutation.isPending}
        fieldErrors={fieldErrors}
        formError={formError}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
