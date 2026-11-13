import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { useAuth } from "../../auth/useAuth";
import { useToast } from "../../shared/components/Toast";
import { deleteSupplier, getSupplier, updateSupplier } from "../suppliersApi";
import { SupplierForm } from "../components/SupplierForm";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { CreateSupplierRequest } from "../types";

export function SupplierDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { hasRole } = useAuth();
  const canManage = hasRole(["ORG_ADMIN", "MANAGER"]);

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["suppliers", "detail", id],
    queryFn: () => getSupplier(id!),
    enabled: !!id,
  });

  const updateMutation = useMutation({
    mutationFn: (values: CreateSupplierRequest) => updateSupplier(id!, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["suppliers", "list"] });
      queryClient.invalidateQueries({ queryKey: ["suppliers", "detail", id] });
      toast.show("Supplier updated", "success");
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
    mutationFn: () => deleteSupplier(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["suppliers", "list"] });
      toast.show("Supplier deleted", "success");
      navigate("/suppliers", { replace: true });
    },
    onError: (error) => {
      toast.show(toErrorMessage(error), "error");
    },
  });

  function handleUpdate(values: CreateSupplierRequest) {
    setFieldErrors({});
    setFormError(null);
    updateMutation.mutate(values);
  }

  function handleDelete() {
    if (window.confirm("Deactivate this supplier? This cannot be undone from here.")) {
      deleteMutation.mutate();
    }
  }

  return (
    <div>
      {query.isLoading && <Spinner label="Loading supplier…" />}
      {query.isError && (
        <div className="error-panel">
          <p>{toErrorMessage(query.error)}</p>
          <Button onClick={() => query.refetch()}>Retry</Button>
        </div>
      )}
      {query.data && (
        <>
          <SupplierForm
            initial={{
              name: query.data.name,
              contactPerson: query.data.contactPerson ?? "",
              email: query.data.email ?? "",
              phone: query.data.phone ?? "",
              address: query.data.address ?? "",
              taxNumber: query.data.taxNumber ?? "",
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
              Delete supplier
            </Button>
          )}
        </>
      )}
    </div>
  );
}
