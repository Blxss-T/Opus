import "@testing-library/jest-dom/vitest";
import axios from "axios";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./auth/__tests__/mockServer";

// jsdom's XMLHttpRequest isn't reliably intercepted by MSW's XHR interceptor,
// which makes axios's default (XHR, since jsdom defines it) adapter throw a
// generic "Network Error" even though a handler matches. Node's own `http`
// adapter is cleanly intercepted by MSW instead, so force it for tests only
// — the real browser build never runs through this setup file.
axios.defaults.adapter = "http";

beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
