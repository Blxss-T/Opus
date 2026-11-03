import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { FormField } from "../../shared/components/FormField";
import { Pager } from "../../shared/components/Pager";
import { usePagination } from "../../shared/hooks/usePagination";
import { useAuth } from "../../auth/useAuth";
import { listProducts } from "../productsApi";
import { ProductTable } from "../components/ProductTable";
import { toErrorMessage } from "../../shared/utils/fieldErrors";

export function ProductListPage() {
  const { hasRole } = useAuth();
  const canManage = hasRole(["ORG_ADMIN", "MANAGER"]);
  const pagination = usePagination();
  const [qInput, setQInput] = useState("");
  const [categoryInput, setCategoryInput] = useState("");
  const [q, setQ] = useState<string | undefined>(undefined);
  const [category, setCategory] = useState<string | undefined>(undefined);

  function handleSearch(e: FormEvent) {
    e.preventDefault();
    setQ(qInput.trim() || undefined);
    setCategory(categoryInput.trim() || undefined);
    pagination.setPage(0);
  }

  const query = useQuery({
    queryKey: [
      "products",
      "list",
      { q, category, page: pagination.page, size: pagination.size, sort: pagination.sort },
    ],
    queryFn: () =>
      listProducts({ q, category, page: pagination.page, size: pagination.size, sort: pagination.sort }),
  });

  return (
    <div>
      <h1>Products</h1>
      <div className="list-toolbar">
        <form onSubmit={handleSearch} className="list-toolbar__search">
          <FormField
            label="Search"
            name="q"
            placeholder="Name or SKU"
            value={qInput}
            onChange={(e) => setQInput(e.target.value)}
          />
          <FormField
            label="Category"
            name="category"
            placeholder="Category"
            value={categoryInput}
            onChange={(e) => setCategoryInput(e.target.value)}
          />
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </form>
        {canManage && (
          <Link to="/products/new" className="btn btn--primary">
            New product
          </Link>
        )}
      </div>

      {query.isLoading && <Spinner label="Loading products…" />}
      {query.isError && (
        <div className="error-panel">
          <p>{toErrorMessage(query.error)}</p>
          <Button onClick={() => query.refetch()}>Retry</Button>
        </div>
      )}
      {query.data && (
        <>
          <ProductTable products={query.data.content} />
          <Pager page={query.data.page} onPrevious={pagination.previousPage} onNext={pagination.nextPage} />
        </>
      )}
    </div>
  );
}
