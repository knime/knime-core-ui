import { vi } from "vitest";

import type { ScriptingServiceType } from "@/scripting-service";

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

export const scriptingServiceMock: ScriptingServiceType = {
  registerEventHandler: vi.fn(),
  sendToService: vi.fn(),
  connectToLanguageServer: vi.fn(),
  isCallKnimeUiApiAvailable: vi.fn(() => Promise.resolve(true)),
  isKaiEnabled: vi.fn(() => Promise.resolve(true)),
  isLoggedIntoHub: vi.fn(() => Promise.resolve(true)),
  getAiDisclaimer: vi.fn(() => Promise.resolve("AI Disclaimer")),
  getAiUsage: vi.fn(() => Promise.resolve({ limit: 100, used: 42 })),
};

export const getScriptingService = vi.fn(() => scriptingServiceMock);
export const initConsoleEventHandler = vi.fn();
export const registerSettingsGetterForApply = vi.fn();
