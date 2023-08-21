import { vi } from "vitest";

export const getScriptingService = vi.fn(() => ({
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
}));
