import type { ScriptingServiceType } from "@/scripting-service";
import { vi } from "vitest";

export const scriptingServiceMock = {
  registerEventHandler: vi.fn(),
  isCodeAssistantEnabled: vi.fn(),
  isCodeAssistantInstalled: vi.fn(),
  inputsAvailable: vi.fn(),
  closeDialog: vi.fn(),
  getFlowVariableInputs: vi.fn(),
  getInputObjects: vi.fn(),
  getOutputObjects: vi.fn(),
  connectToLanguageServer: vi.fn(),
  sendToService: vi.fn(),
  getInitialSettings: vi.fn(() =>
    Promise.resolve({ script: "myInitialScript" }),
  ),
  registerSettingsGetterForApply: vi.fn(),
} as ScriptingServiceType;

export const getScriptingService = vi.fn(() => scriptingServiceMock);
export const initConsoleEventHandler = vi.fn();
export const registerSettingsGetterForApply = vi.fn();
