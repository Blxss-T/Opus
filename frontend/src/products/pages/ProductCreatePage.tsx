import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createProduct } from "../productsApi";
import { ProductForm, type ProductFormValues } from "../components/ProductForm";
import { useToast } from "../../shared/components/Toast";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { CreateProductRequest } from "../types";

const initialForm: ProductFormValues = {
  sku: "",
  name: "",
  description: "",
  category: "",
  unitPrice: 0,
  costPrice: 0,
  initialStock: 0,
  reorderLevel: 10,
};

export function ProductCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createProduct,
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ["products", "list"] });
      toast.show("Product created", "success");
      navigate(`/products/${created.id}`, { replace: true });
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

  function handleSubmit(values: CreateProductRequest) {
    setFieldErrors({});
    setFormError(null);
    mutation.mutate(values);
  }

  return (
    <div>
      <h1>New product</h1>
      <ProductForm
        mode="create"
        initial={initialForm}
        submitLabel="Create product"
        submitting={mutation.isPending}
        fieldErrors={fieldErrors}
        formError={formError}
        onSubmit={(values) => handleSubmit(values as CreateProductRequest)}
      />
    </div>
  );
}
