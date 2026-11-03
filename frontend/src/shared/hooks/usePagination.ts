import { useMemo, useState } from "react";

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
}

export interface PaginationControls extends PaginationParams {
  setPage: (page: number) => void;
  setSize: (size: number) => void;
  setSort: (sort: string | undefined) => void;
  nextPage: () => void;
  previousPage: () => void;
}

// Shared by future paginated list screens (products, customers, orders, ...)
// to drive the `page`/`size`/`sort` query params every list endpoint accepts.
export function usePagination(initial: Partial<PaginationParams> = {}): PaginationControls {
  const [page, setPage] = useState(initial.page ?? 0);
  const [size, setSize] = useState(initial.size ?? 20);
  const [sort, setSort] = useState<string | undefined>(initial.sort);

  return useMemo(
    () => ({
      page,
      size,
      sort,
      setPage,
      setSize,
      setSort,
      nextPage: () => setPage((current) => current + 1),
      previousPage: () => setPage((current) => Math.max(0, current - 1)),
    }),
    [page, size, sort],
  );
}
