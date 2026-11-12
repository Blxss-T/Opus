import "@testing-library/jest-dom/vitest";
import axios from "axios";
import { afterAll, afterEach, beforeAll, beforeEach } from "vitest";
import { httpClient } from "./api/client";
import { resetAuthStore } from "./auth/authStore";
import { clearTokens } from "./auth/tokenStorage";
import { server } from "./auth/__tests__/mockServer";

axios.defaults.adapter = "http";
httpClient.defaults.adapter = "http";

if (!HTMLElement.prototype.hasPointerCapture) {
  HTMLElement.prototype.hasPointerCapture = () => false;
}
if (!HTMLElement.prototype.setPointerCapture) {
  HTMLElement.prototype.setPointerCapture = () => {};
}
if (!HTMLElement.prototype.releasePointerCapture) {
  HTMLElement.prototype.releasePointerCapture = () => {};
}
if (!HTMLElement.prototype.scrollIntoView) {
  HTMLElement.prototype.scrollIntoView = () => {};
}

class ResizeObserverStub {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}
globalThis.ResizeObserver = ResizeObserverStub;

beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

beforeEach(() => {
  clearTokens();
  resetAuthStore();
});
