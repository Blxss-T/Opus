import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/testSetup.ts"],
    // Keep axios on Node's HTTP adapter so MSW can intercept. Vite's browser
    // resolve map would otherwise replace the http adapter with a stub and
    // jsdom would emit CORS OPTIONS that no handler matches.
    server: {
      deps: {
        external: ["axios"],
      },
    },
  },
});
