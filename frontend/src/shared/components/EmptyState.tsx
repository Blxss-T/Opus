import { Inbox } from "lucide-react";

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      <span className="flex h-10 w-10 items-center justify-center rounded-full bg-chip text-muted">
        <Inbox size={20} strokeWidth={1.75} aria-hidden />
      </span>
      <p className="text-small text-muted">{message}</p>
    </div>
  );
}
