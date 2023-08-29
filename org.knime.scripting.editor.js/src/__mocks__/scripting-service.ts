import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  registerConsoleEventHandler: vi.fn(),
  saveSettings: vi.fn(() => {}),
  initEditorService: vi.fn(() => {}),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
