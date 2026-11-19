import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import type { ReactNode } from "react";
import { useLocation } from "react-router-dom";
import { BrandLogo } from "../../shared/components/BrandLogo";
import { fadeIn, motionEase } from "../../shared/motion";
import { cn } from "../../shared/utils/cn";

interface PanelCopy {
  heading: string;
  body: string;
  steps: { label: string; active: boolean }[];
}

function panelFor(pathname: string): PanelCopy {
  if (pathname.startsWith("/register")) {
    return {
      heading: "Get started with us",
      body: "Complete these easy steps to register your organization.",
      steps: [
        { label: "Sign up your account", active: true },
        { label: "Set up your workspace", active: false },
        { label: "Invite your team", active: false },
      ],
    };
  }
  if (pathname.startsWith("/password/forgot")) {
    return {
      heading: "Reset access",
      body: "We will send a code to the email on your account.",
      steps: [
        { label: "Request a reset code", active: true },
        { label: "Choose a new password", active: false },
      ],
    };
  }
  if (pathname.startsWith("/password/reset")) {
    return {
      heading: "Reset access",
      body: "Enter the code from your email and a new password.",
      steps: [
        { label: "Request a reset code", active: false },
        { label: "Choose a new password", active: true },
      ],
    };
  }
  if (pathname.startsWith("/verify-otp")) {
    return {
      heading: "Verify your email",
      body: "Confirm the code so you can open your workspace.",
      steps: [
        { label: "Verify your email", active: true },
        { label: "Open your workspace", active: false },
      ],
    };
  }
  return {
    heading: "Welcome back",
    body: "Sign in to keep running purchasing, sales, and stock in one place.",
    steps: [
      { label: "Sign in to your account", active: true },
      { label: "Open your workspace", active: false },
      { label: "Work with your team", active: false },
    ],
  };
}

function BrandPanel({ copy }: { copy: PanelCopy }) {
  return (
    <div className="hidden h-full w-1/2 bg-brand-deep p-4 md:flex">
      <aside className="relative flex w-full flex-col overflow-hidden rounded-xl p-10 text-white">
        <img
          src="/side_background.png"
          alt=""
          className="pointer-events-none absolute inset-0 h-full w-full scale-y-[-1] object-cover"
        />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-brand-deep/80 via-brand-deep/25 to-transparent" />
        <BrandLogo onDark className="relative z-10 mx-auto -mt-4 h-44 w-auto max-w-[90%]" />
        <div className="relative z-10 mt-auto">
          <h2 className="m-0 text-center text-page font-semibold text-white">{copy.heading}</h2>
          <p className="mx-auto mt-3 mb-10 max-w-xs text-center text-body text-white/75">{copy.body}</p>
          <ol className="m-0 flex list-none flex-col gap-3 p-0">
            {copy.steps.map((step, index) => (
              <li
                key={step.label}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-4 py-3.5 font-sans text-body font-medium",
                  step.active ? "bg-white text-heading" : "bg-brand-step text-white",
                )}
              >
                <span
                  className={cn(
                    "flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-caption font-semibold",
                    step.active ? "bg-heading text-white" : "bg-white/15 text-white",
                  )}
                >
                  {index + 1}
                </span>
                {step.label}
              </li>
            ))}
          </ol>
        </div>
      </aside>
    </div>
  );
}

export function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
}) {
  const location = useLocation();
  const reduceMotion = useReducedMotion();
  const copy = panelFor(location.pathname);

  return (
    <div className="flex h-dvh w-full overflow-hidden bg-auth-bg">
      <BrandPanel copy={copy} />
      <section className="auth-dark flex min-h-0 min-w-0 flex-1 flex-col">
          <div className="flex h-14 items-center bg-auth-bg px-4 md:hidden">
            <BrandLogo onDark className="h-10" />
          </div>
          <div className="flex min-h-0 flex-1 items-center justify-center overflow-y-auto px-6 py-10 md:px-16">
            <div className="w-full max-w-[420px]">
              <AnimatePresence mode="wait">
                <motion.div
                  key={location.pathname}
                  variants={fadeIn}
                  initial={reduceMotion ? false : "initial"}
                  animate="animate"
                  exit="exit"
                  transition={reduceMotion ? { duration: 0 } : motionEase}
                >
                  <h1 className="m-0 text-center text-page text-heading">{title}</h1>
                  {subtitle && <p className="mt-2 mb-8 text-center text-small text-muted">{subtitle}</p>}
                  {!subtitle && <div className="mb-8" />}
                  {children}
                </motion.div>
              </AnimatePresence>
            </div>
          </div>
        </section>
    </div>
  );
}
