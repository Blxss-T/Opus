import type { ReactNode } from "react";
import { cn } from "../utils/cn";

export type StatusTone = "draft" | "ordered" | "success" | "warning" | "danger" | "neutral";

const toneClass: Record<StatusTone, string> = {
  draft: "bg-chip text-muted",
  ordered: "bg-status-ordered-soft text-status-ordered",
  success: "bg-success-soft text-success",
  warning: "bg-warning-soft text-warning",
  danger: "bg-danger-soft text-danger",
  neutral: "bg-chip text-body",
};

export function statusToneFor(status: string): StatusTone {
  switch (status) {
    case "DRAFT":
    case "INACTIVE":
    case "TERMINATED":
      return "draft";
    case "ORDERED":
    case "CONFIRMED":
    case "PENDING":
      return "ordered";
    case "RECEIVED":
    case "FULFILLED":
    case "ACTIVE":
      return "success";
    case "CANCELLED":
      return "danger";
    default:
      return "neutral";
  }
}

export function StatusPill({
  tone,
  children,
  className,
}: {
  tone: StatusTone;
  children: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-caption font-semibold tracking-wide",
        toneClass[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
