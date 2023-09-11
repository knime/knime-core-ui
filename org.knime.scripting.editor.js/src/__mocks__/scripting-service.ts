import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  registerConsoleEventHandler: vi.fn(),
  saveSettings: vi.fn(() => {}),
  initEditorService: vi.fn(() => {}),
  registerLanguageServerEventHandler: vi.fn(),
  getScript: vi.fn(() => {}),
  setScript: vi.fn(),
  registerEventHandler: vi.fn(),
  sendToService: vi.fn(() => {}),
  pasteToEditor: vi.fn(() => {}),
  supportsCodeAssistant: vi.fn(),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
