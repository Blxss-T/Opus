import * as Dialog from "@radix-ui/react-dialog";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { modalPop, motionEase, overlayFade } from "../motion";
import { IconButton } from "./IconButton";

interface ModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
}

export function Modal({ open, onOpenChange, title, description, children }: ModalProps) {
  const reduceMotion = useReducedMotion();
  const transition = reduceMotion ? { duration: 0 } : motionEase;

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <AnimatePresence>
        {open && (
          <Dialog.Portal forceMount>
            <Dialog.Overlay asChild>
              <motion.div
                className="fixed inset-0 z-40 bg-heading/40"
                variants={overlayFade}
                initial={reduceMotion ? false : "initial"}
                animate="animate"
                exit="exit"
                transition={transition}
              />
            </Dialog.Overlay>
            <Dialog.Content asChild>
              <motion.div
                className="fixed top-1/2 left-1/2 z-50 w-[min(32rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2 rounded-lg bg-surface p-6 shadow-md focus:outline-none"
                variants={modalPop}
                initial={reduceMotion ? false : "initial"}
                animate="animate"
                exit="exit"
                transition={transition}
              >
                <div className="mb-4 flex items-start justify-between gap-4">
                  <div>
                    <Dialog.Title className="text-card text-heading">{title}</Dialog.Title>
                    {description && (
                      <Dialog.Description className="mt-1 text-small text-muted">{description}</Dialog.Description>
                    )}
                  </div>
                  <Dialog.Close asChild>
                    <IconButton label="Close">
                      <X size={18} strokeWidth={1.75} aria-hidden />
                    </IconButton>
                  </Dialog.Close>
                </div>
                {children}
              </motion.div>
            </Dialog.Content>
          </Dialog.Portal>
        )}
      </AnimatePresence>
    </Dialog.Root>
  );
}
