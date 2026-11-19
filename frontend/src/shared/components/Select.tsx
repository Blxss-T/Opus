import * as SelectPrimitive from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";
import * as Label from "@radix-ui/react-label";
import { cn } from "../utils/cn";

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps {
  label?: string;
  name?: string;
  value: string;
  onValueChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
  error?: string;
  id?: string;
}

export function Select({
  label,
  name,
  value,
  onValueChange,
  options,
  placeholder = "Select",
  disabled,
  error,
  id,
}: SelectProps) {
  const fieldId = id ?? name;
  return (
    <div className="mb-4">
      {label && (
        <Label.Root htmlFor={fieldId} className="mb-1 block text-small font-semibold text-heading">
          {label}
        </Label.Root>
      )}
      <SelectPrimitive.Root value={value} onValueChange={onValueChange} disabled={disabled} name={name}>
        <SelectPrimitive.Trigger
          id={fieldId}
          aria-invalid={error ? true : undefined}
          className={cn(
            "flex h-10 w-full items-center justify-between rounded-md border border-border bg-surface px-3 text-body text-heading",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600",
            "disabled:cursor-not-allowed disabled:opacity-60",
            "data-placeholder:text-muted",
          )}
        >
          <SelectPrimitive.Value placeholder={placeholder} />
          <SelectPrimitive.Icon>
            <ChevronDown size={16} strokeWidth={1.75} className="text-muted" aria-hidden />
          </SelectPrimitive.Icon>
        </SelectPrimitive.Trigger>
        <SelectPrimitive.Portal>
          <SelectPrimitive.Content
            position="popper"
            sideOffset={4}
            className="z-50 max-h-64 min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-md border border-border bg-surface shadow-md"
          >
            <SelectPrimitive.Viewport className="p-1">
              {options.map((option) => (
                <SelectPrimitive.Item
                  key={option.value}
                  value={option.value}
                  className={cn(
                    "relative flex cursor-pointer items-center rounded-sm py-2 pr-8 pl-2 text-small text-heading outline-none",
                    "data-highlighted:bg-primary-50 data-highlighted:text-primary-700",
                  )}
                >
                  <SelectPrimitive.ItemText>{option.label}</SelectPrimitive.ItemText>
                  <SelectPrimitive.ItemIndicator className="absolute right-2">
                    <Check size={16} strokeWidth={1.75} className="text-primary-600" aria-hidden />
                  </SelectPrimitive.ItemIndicator>
                </SelectPrimitive.Item>
              ))}
            </SelectPrimitive.Viewport>
          </SelectPrimitive.Content>
        </SelectPrimitive.Portal>
      </SelectPrimitive.Root>
      {error && (
        <p className="mt-1 text-caption text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
