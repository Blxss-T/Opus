import type { ReactNode } from "react";

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-4">
      <div className="w-full max-w-[400px] rounded-lg bg-surface p-8 shadow-md">
        <p className="mb-1 text-small font-semibold tracking-wide text-primary-600">Opus</p>
        <h1 className="mb-6 text-page text-heading">{title}</h1>
        {children}
      </div>
    </div>
  );
}
