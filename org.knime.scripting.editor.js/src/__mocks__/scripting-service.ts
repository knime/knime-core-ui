import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  registerConsoleEventHandler: vi.fn(),
  saveSettings: vi.fn(() => {}),
  initEditorService: vi.fn(() => {}),
  registerLanguageServerEventHandler: vi.fn(),
  getScript: vi.fn(),
  setScript: vi.fn(),
  registerEventHandler: vi.fn(),
  sendToService: vi.fn(() => {}),
  pasteToEditor: vi.fn(() => {}),
  isCodeAssistantEnabled: vi.fn(),
  isCodeAssistantInstalled: vi.fn(),
  inputsAvailable: vi.fn(),
  closeDialog: vi.fn(),
  getFlowVariableInputs: vi.fn(),
  getInputObjects: vi.fn(),
  getOutputObjects: vi.fn(),
  setOnDidChangeContentListener: vi.fn(),
  initClearConsoleCallback: vi.fn(),
  clearConsole: vi.fn(),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
