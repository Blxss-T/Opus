import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Spinner } from "../../shared/components/Spinner";
import { Button } from "../../shared/components/Button";
import { FormField } from "../../shared/components/FormField";
import { Pager } from "../../shared/components/Pager";
import { usePagination } from "../../shared/hooks/usePagination";
import { useAuth } from "../../auth/useAuth";
import { listSuppliers } from "../suppliersApi";
import { SupplierTable } from "../components/SupplierTable";
import { toErrorMessage } from "../../shared/utils/fieldErrors";

export function SupplierListPage() {
  const { hasRole } = useAuth();
  const canManage = hasRole(["ORG_ADMIN", "MANAGER"]);
  const pagination = usePagination();
  const [qInput, setQInput] = useState("");
  const [q, setQ] = useState<string | undefined>(undefined);

  function handleSearch(e: FormEvent) {
    e.preventDefault();
    setQ(qInput.trim() || undefined);
    pagination.setPage(0);
  }

  const query = useQuery({
    queryKey: ["suppliers", "list", { q, page: pagination.page, size: pagination.size, sort: pagination.sort }],
    queryFn: () => listSuppliers({ q, page: pagination.page, size: pagination.size, sort: pagination.sort }),
  });

  return (
    <div>
      <div className="list-toolbar">
        <form onSubmit={handleSearch} className="list-toolbar__search">
          <FormField
            label="Search"
            name="q"
            placeholder="Name, email, or contact"
            value={qInput}
            onChange={(e) => setQInput(e.target.value)}
          />
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </form>
        {canManage && (
          <Link to="/suppliers/new" className="btn btn--primary">
            New supplier
          </Link>
        )}
      </div>

      {query.isLoading && <Spinner label="Loading suppliers…" />}
      {query.isError && (
        <div className="error-panel">
          <p>{toErrorMessage(query.error)}</p>
          <Button onClick={() => query.refetch()}>Retry</Button>
        </div>
      )}
      {query.data && (
        <>
          <SupplierTable suppliers={query.data.content} />
          <Pager page={query.data.page} onPrevious={pagination.previousPage} onNext={pagination.nextPage} />
        </>
      )}
    </div>
  );
}
