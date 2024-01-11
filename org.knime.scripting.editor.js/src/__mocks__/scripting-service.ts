import type { ScriptingServiceType } from "@/scripting-service";
import { vi } from "vitest";

export const scriptingServiceMock = {
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  registerConsoleEventHandler: vi.fn(),
  registerLanguageServerEventHandler: vi.fn(),
  registerEventHandler: vi.fn(),
  isCodeAssistantEnabled: vi.fn(),
  isCodeAssistantInstalled: vi.fn(),
  inputsAvailable: vi.fn(),
  closeDialog: vi.fn(),
  getFlowVariableInputs: vi.fn(),
  getInputObjects: vi.fn(),
  getOutputObjects: vi.fn(),
  configureLanguageServer: vi.fn(),
  connectToLanguageServer: vi.fn(),
  saveSettings: vi.fn(),
  sendToService: vi.fn(),
} as ScriptingServiceType;

export const getScriptingService = vi.fn(() => scriptingServiceMock);
