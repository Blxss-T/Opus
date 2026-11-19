export function Spinner({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 py-4" role="status" aria-live="polite">
      <span
        className="h-[18px] w-[18px] animate-spin rounded-full border-2 border-border border-t-primary-600"
        aria-hidden="true"
      />
      <span className="text-small text-muted">{label}</span>
    </div>
  );
}
