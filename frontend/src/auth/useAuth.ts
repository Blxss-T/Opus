import { useAuthStore, type AuthState } from "./authStore";

export function useAuth(): AuthState {
  return useAuthStore();
}
