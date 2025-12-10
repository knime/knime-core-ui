import { vi } from "vitest";
import { consola } from "consola";

import { initMocked } from "../init";
import { DEFAULT_INITIAL_DATA } from "../initial-data-service-browser-mock";
import type { GenericNodeSettings } from "../settings-service";
import { DEFAULT_INITIAL_SETTINGS } from "../settings-service-browser-mock";

vi.mock("monaco-editor");

window.consola = consola;

vi.mock("@xterm/xterm", () => {
  const Terminal = vi.fn();

  Terminal.prototype.open = vi.fn();
  Terminal.prototype.write = vi.fn();
  Terminal.prototype.writeln = vi.fn();
  Terminal.prototype.loadAddon = vi.fn();
  Terminal.prototype.reset = vi.fn();
  Terminal.prototype.onLineFeed = vi.fn();
  Terminal.prototype.unicode = vi.fn();
  Terminal.prototype.attachCustomKeyEventHandler = vi.fn();
  Terminal.prototype.onWriteParsed = vi.fn();
  Terminal.prototype.scrollToBottom = vi.fn();
  Terminal.prototype.unicodeSerivce = vi.fn();

  return { Terminal };
});

initMocked({
  scriptingService: {
    sendToService: vi.fn(),
    getOutputPreviewTableInitialData: vi.fn(() => Promise.resolve(undefined)),
    registerEventHandler: vi.fn(),
    connectToLanguageServer: vi.fn(),
    isCallKnimeUiApiAvailable: vi.fn(() => Promise.resolve(true)),
    isKaiEnabled: vi.fn(() => Promise.resolve(true)),
    isLoggedIntoHub: vi.fn(() => Promise.resolve(true)),
    getAiDisclaimer: vi.fn(() => Promise.resolve("AI Disclaimer")),
    getAiUsage: vi.fn(() => Promise.resolve({ type: "UNKNOWN" } as const)),
    sendAlert: vi.fn(),
  },
  initialData: DEFAULT_INITIAL_DATA,
  settingsService: {
    getSettings: vi.fn(() => DEFAULT_INITIAL_SETTINGS),
    registerSettingsGetterForApply: vi.fn(
      (_settingsGetter: () => GenericNodeSettings) => {},
    ),
    registerSettings: vi.fn(() => vi.fn()),
  },
  displayMode: "large",
});
