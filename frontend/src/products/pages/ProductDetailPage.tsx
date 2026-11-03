import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { Pager } from "../../shared/components/Pager";
import { usePagination } from "../../shared/hooks/usePagination";
import { useAuth } from "../../auth/useAuth";
import { useToast } from "../../shared/components/Toast";
import { deleteProduct, getProduct, getMovements, adjustStock, updateProduct } from "../productsApi";
import { ProductForm } from "../components/ProductForm";
import { AdjustStockForm } from "../components/AdjustStockForm";
import { MovementsTable } from "../components/MovementsTable";
import { toErrorMessage, toFieldErrorMap } from "../../shared/utils/fieldErrors";
import type { AdjustStockRequest, MovementType, UpdateProductRequest } from "../types";

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { hasRole } = useAuth();
  const canManage = hasRole(["ORG_ADMIN", "MANAGER"]);

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [adjustError, setAdjustError] = useState<string | null>(null);
  const [movementType, setMovementType] = useState<MovementType | undefined>(undefined);
  const movementsPagination = usePagination();

  const productQuery = useQuery({
    queryKey: ["products", "detail", id],
    queryFn: () => getProduct(id!),
    enabled: !!id,
  });

  const movementsQuery = useQuery({
    queryKey: [
      "products",
      id,
      "movements",
      { type: movementType, page: movementsPagination.page, size: movementsPagination.size, sort: movementsPagination.sort },
    ],
    queryFn: () =>
      getMovements(id!, {
        type: movementType,
        page: movementsPagination.page,
        size: movementsPagination.size,
        sort: movementsPagination.sort,
      }),
    enabled: !!id,
  });

  const updateMutation = useMutation({
    mutationFn: (values: UpdateProductRequest) => updateProduct(id!, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "list"] });
      queryClient.invalidateQueries({ queryKey: ["products", "detail", id] });
      toast.show("Product updated", "success");
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
    mutationFn: () => deleteProduct(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "list"] });
      toast.show("Product deleted", "success");
      navigate("/products", { replace: true });
    },
    onError: (error) => {
      toast.show(toErrorMessage(error), "error");
    },
  });

  const adjustStockMutation = useMutation({
    mutationFn: (values: AdjustStockRequest) => adjustStock(id!, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products", "detail", id] });
      queryClient.invalidateQueries({ queryKey: ["products", id, "movements"] });
      toast.show("Stock adjusted", "success");
      setAdjustError(null);
    },
    onError: (error) => {
      setAdjustError(toErrorMessage(error));
    },
  });

  function handleUpdate(values: UpdateProductRequest) {
    setFieldErrors({});
    setFormError(null);
    updateMutation.mutate(values);
  }

  function handleDelete() {
    if (window.confirm("Deactivate this product? This cannot be undone from here.")) {
      deleteMutation.mutate();
    }
  }

  return (
    <div>
      <h1>Product</h1>
      {productQuery.isLoading && <Spinner label="Loading product…" />}
      {productQuery.isError && (
        <div className="error-panel">
          <p>{toErrorMessage(productQuery.error)}</p>
          <Button onClick={() => productQuery.refetch()}>Retry</Button>
        </div>
      )}
      {productQuery.data && (
        <>
          <ProductForm
            mode="edit"
            initial={{
              sku: productQuery.data.sku,
              name: productQuery.data.name,
              description: productQuery.data.description ?? "",
              category: productQuery.data.category,
              unitPrice: productQuery.data.unitPrice,
              costPrice: productQuery.data.costPrice,
              initialStock: 0,
              reorderLevel: productQuery.data.reorderLevel,
            }}
            submitLabel="Save changes"
            submitting={updateMutation.isPending}
            fieldErrors={fieldErrors}
            formError={formError}
            readOnly={!canManage}
            onSubmit={(values) => handleUpdate(values as UpdateProductRequest)}
          />
          {canManage && (
            <Button variant="danger" onClick={handleDelete} loading={deleteMutation.isPending}>
              Delete product
            </Button>
          )}

          {canManage && (
            <section>
              <h2>Adjust stock</h2>
              <p>Current stock: {productQuery.data.stockQuantity}</p>
              <AdjustStockForm
                submitting={adjustStockMutation.isPending}
                formError={adjustError}
                onSubmit={(values) => adjustStockMutation.mutate(values)}
              />
            </section>
          )}

          <section>
            <h2>Stock movements</h2>
            {movementsQuery.isLoading && <Spinner label="Loading movements…" />}
            {movementsQuery.isError && (
              <div className="error-panel">
                <p>{toErrorMessage(movementsQuery.error)}</p>
                <Button onClick={() => movementsQuery.refetch()}>Retry</Button>
              </div>
            )}
            {movementsQuery.data && (
              <>
                <MovementsTable
                  movements={movementsQuery.data.content}
                  typeFilter={movementType}
                  onTypeFilterChange={(type) => {
                    setMovementType(type);
                    movementsPagination.setPage(0);
                  }}
                />
                <Pager
                  page={movementsQuery.data.page}
                  onPrevious={movementsPagination.previousPage}
                  onNext={movementsPagination.nextPage}
                />
              </>
            )}
          </section>
        </>
      )}
    </div>
  );
}
