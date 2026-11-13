import { useEffect, type ReactNode } from "react";
import { useAuthStore } from "./authStore";

export function AuthProvider({ children }: { children: ReactNode }) {
  const hydrate = useAuthStore((state) => state.hydrate);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return children;
}

export { useAuth as useAuthContext } from "./useAuth";
