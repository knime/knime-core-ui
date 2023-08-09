import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  saveSettings: vi.fn(() => {}),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
