import * as Dialog from "@radix-ui/react-dialog";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { motionEase, overlayFade } from "../shared/motion";
import { NavSidebar } from "../nav/NavSidebar";

export function MobileNavDrawer({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
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
            <Dialog.Content asChild aria-describedby={undefined}>
              <motion.div
                className="fixed inset-y-0 left-0 z-50 h-full outline-none"
                initial={reduceMotion ? false : { x: -24, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                exit={{ x: -24, opacity: 0 }}
                transition={transition}
              >
                <Dialog.Title className="sr-only">Navigation</Dialog.Title>
                <NavSidebar collapsed={false} onNavigate={() => onOpenChange(false)} />
              </motion.div>
            </Dialog.Content>
          </Dialog.Portal>
        )}
      </AnimatePresence>
    </Dialog.Root>
  );
}
