import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { useAuth } from "../../auth/useAuth";
import { useToast } from "../../shared/components/Toast";
import { deleteCustomer, getCustomer, updateCustomer } from "../customersApi";
import { CustomerForm } from "../components/CustomerForm";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { CreateCustomerRequest } from "../types";

export function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { hasRole } = useAuth();
  const canManage = hasRole(["ORG_ADMIN", "MANAGER"]);

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["customers", "detail", id],
    queryFn: () => getCustomer(id!),
    enabled: !!id,
  });

  const updateMutation = useMutation({
    mutationFn: (values: CreateCustomerRequest) => updateCustomer(id!, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers", "list"] });
      queryClient.invalidateQueries({ queryKey: ["customers", "detail", id] });
      toast.show("Customer updated", "success");
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

  const deleteMutation = useMutation({
    mutationFn: () => deleteCustomer(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers", "list"] });
      toast.show("Customer deleted", "success");
      navigate("/customers", { replace: true });
    },
    onError: (error) => {
      toast.show(toErrorMessage(error), "error");
    },
  });

  function handleUpdate(values: CreateCustomerRequest) {
    setFieldErrors({});
    setFormError(null);
    updateMutation.mutate(values);
  }

  function handleDelete() {
    if (window.confirm("Deactivate this customer? This cannot be undone from here.")) {
      deleteMutation.mutate();
    }
  }

  return (
    <div>
      <h1>Customer</h1>
      {query.isLoading && <Spinner label="Loading customer…" />}
      {query.isError && (
        <div className="error-panel">
          <p>{toErrorMessage(query.error)}</p>
          <Button onClick={() => query.refetch()}>Retry</Button>
        </div>
      )}
      {query.data && (
        <>
          <CustomerForm
            initial={{
              name: query.data.name,
              email: query.data.email,
              phone: query.data.phone ?? "",
              address: query.data.address ?? "",
              notes: query.data.notes ?? "",
            }}
            submitLabel="Save changes"
            submitting={updateMutation.isPending}
            fieldErrors={fieldErrors}
            formError={formError}
            readOnly={!canManage}
            onSubmit={handleUpdate}
          />
          {canManage && (
            <Button variant="danger" onClick={handleDelete} loading={deleteMutation.isPending}>
              Delete customer
            </Button>
          )}
        </>
      )}
    </div>
  );
}
