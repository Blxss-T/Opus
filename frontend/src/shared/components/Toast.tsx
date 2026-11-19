import { AnimatePresence, motion } from "framer-motion";
import { CheckCircle2, CircleAlert, Info, X } from "lucide-react";
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { motionEase, toastSlide } from "../motion";
import { cn } from "../utils/cn";
import { IconButton } from "./IconButton";

type ToastKind = "info" | "error" | "success";

interface ToastMessage {
  id: number;
  text: string;
  kind: ToastKind;
}

interface ToastContextValue {
  show: (text: string, kind?: ToastKind) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 1;

const kindIcon: Record<ToastKind, typeof Info> = {
  info: Info,
  error: CircleAlert,
  success: CheckCircle2,
};

const kindClass: Record<ToastKind, string> = {
  info: "border-border",
  error: "border-danger",
  success: "border-success",
};

const kindIconClass: Record<ToastKind, string> = {
  info: "text-primary-600",
  error: "text-danger",
  success: "text-success",
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const show = useCallback((text: string, kind: ToastKind = "info") => {
    const id = nextId++;
    setToasts((current) => [...current, { id, text, kind }]);
    setTimeout(() => dismiss(id), 4000);
  }, [dismiss]);

  const value = useMemo<ToastContextValue>(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed right-4 bottom-4 z-50 flex flex-col gap-2" aria-live="polite">
        <AnimatePresence>
          {toasts.map((toast) => {
            const Icon = kindIcon[toast.kind];
            return (
              <motion.div
                key={toast.id}
                className={cn(
                  "flex min-w-64 items-start gap-3 rounded-md border bg-surface px-3 py-2.5 shadow-md",
                  kindClass[toast.kind],
                )}
                variants={toastSlide}
                initial="initial"
                animate="animate"
                exit="exit"
                transition={motionEase}
                role={toast.kind === "error" ? "alert" : "status"}
              >
                <Icon size={18} strokeWidth={1.75} className={cn("mt-0.5 shrink-0", kindIconClass[toast.kind])} aria-hidden />
                <p className="flex-1 text-small text-heading">{toast.text}</p>
                <IconButton label="Dismiss notification" className="h-7 w-7" onClick={() => dismiss(toast.id)}>
                  <X size={16} strokeWidth={1.75} aria-hidden />
                </IconButton>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
