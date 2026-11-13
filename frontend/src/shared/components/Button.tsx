import { Slot } from "@radix-ui/react-slot";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "../utils/cn";

type Variant = "primary" | "secondary" | "danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  asChild?: boolean;
  children: ReactNode;
}

const variantClass: Record<Variant, string> = {
  primary:
    "bg-primary-600 text-white hover:bg-primary-700 disabled:hover:bg-primary-600",
  secondary:
    "bg-surface text-heading border border-border hover:bg-chip disabled:hover:bg-surface",
  danger: "bg-danger text-white hover:bg-danger/90 disabled:hover:bg-danger",
};

export function Button({
  variant = "primary",
  loading,
  disabled,
  asChild,
  children,
  className,
  type,
  ...rest
}: ButtonProps) {
  const Comp = asChild ? Slot : "button";
  return (
    <Comp
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-body font-semibold transition-colors duration-150",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2",
        "disabled:cursor-not-allowed disabled:opacity-60",
        "active:scale-[0.98] motion-reduce:active:scale-100",
        variantClass[variant],
        className,
      )}
      disabled={disabled || loading}
      type={asChild ? undefined : (type ?? "button")}
      {...rest}
    >
      {loading ? "Please wait…" : children}
    </Comp>
  );
}
