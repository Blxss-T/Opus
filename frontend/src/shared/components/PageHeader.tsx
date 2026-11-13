import type { ReactNode } from "react";

export function PageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-1 items-center justify-between gap-4">
      <h1 className="m-0 truncate text-page text-heading">{title}</h1>
      {actions}
    </div>
  );
}
