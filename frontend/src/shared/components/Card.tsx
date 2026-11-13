import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "../utils/cn";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ children, className, ...rest }: CardProps) {
  return (
    <div
      className={cn("rounded-lg bg-surface p-6 shadow-sm", className)}
      {...rest}
    >
      {children}
    </div>
  );
}
