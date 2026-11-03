import type { ReactNode } from "react";

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="auth-layout">
      <div className="auth-layout__card">
        <h1 className="auth-layout__brand">Opus</h1>
        <h2 className="auth-layout__title">{title}</h2>
        {children}
      </div>
    </div>
  );
}
