import { vi } from "vitest";

export const defaultPortConfig = {
  inputPorts: [
    {
      nodeId: "root",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
    {
      nodeId: "notRoot",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
  ],
};

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
  getPortConfigs: vi.fn(() => Promise.resolve(defaultPortConfig)),
  isCallKnimeUiApiAvailable: vi.fn(() => Promise.resolve(true)),
  registerSettingsGetterForApply: vi.fn(),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
export const initConsoleEventHandler = vi.fn();
export const registerSettingsGetterForApply = vi.fn();
