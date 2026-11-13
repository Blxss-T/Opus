import * as Label from "@radix-ui/react-label";
import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "../utils/cn";
import { Input } from "./Input";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  children?: ReactNode;
}

export function FormField({ label, error, id, className, children, ...rest }: FormFieldProps) {
  const fieldId = id ?? rest.name;
  return (
    <div className="mb-4">
      <Label.Root htmlFor={fieldId} className="mb-1 block text-small font-semibold text-heading">
        {label}
      </Label.Root>
      {children ?? <Input id={fieldId} className={className} aria-invalid={error ? true : undefined} {...rest} />}
      {error && (
        <p className="mt-1 text-caption text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }
  return (
    <p className={cn("mt-1 text-caption text-danger")} role="alert">
      {message}
    </p>
  );
}
