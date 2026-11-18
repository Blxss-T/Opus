import { cn } from "../utils/cn";

interface BrandLogoProps {
  className?: string;
  /** Knock out the black canvas so the silver mark sits on a dark panel. */
  onDark?: boolean;
}

export function BrandLogo({ className, onDark = false }: BrandLogoProps) {
  return (
    <img
      src="/opus_logo.png"
      alt="Opus"
      className={cn(
        "h-10 w-auto object-contain",
        onDark ? "mix-blend-screen" : "invert mix-blend-multiply",
        className,
      )}
    />
  );
}
