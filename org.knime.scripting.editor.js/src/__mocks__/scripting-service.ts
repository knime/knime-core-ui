import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  sendToService: vi.fn(),
  registerConsoleEventHandler: vi.fn(),
  saveSettings: vi.fn(() => {}),
  initEditorService: vi.fn(() => {}),
  registerLanguageServerEventHandler: vi.fn(),
  getScript: vi.fn(() => {}),
  setScript: vi.fn(),
  registerEventHandler: vi.fn(),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
