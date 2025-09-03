import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { displayMode } from "@/display-mode";
import {
  getInitialData,
  getScriptingService,
  getSettingsService,
  init,
} from "@/init";

const { dialogServiceInstance, jsonDataServiceInstance } = vi.hoisted(() => {
  const dialogServiceInstance = {
    setApplyListener: vi.fn(),
    getInitialDisplayMode: vi.fn(),
    addOnDisplayModeChangeCallback: vi.fn(),
  };

  const jsonDataServiceInstance = {
    initialData: vi.fn(),
    data: vi.fn(),
  };

  vi.resetModules();
  vi.doMock("@knime/ui-extension-service", () => ({
    JsonDataService: {
      getInstance: vi.fn(() => Promise.resolve(jsonDataServiceInstance)),
    },
    DialogService: {
      getInstance: vi.fn(() => Promise.resolve(dialogServiceInstance)),
    },
  }));

  vi.doMock("@/scripting-service", () => {
    class MockScriptingService {}
    return { ScriptingService: MockScriptingService };
  });

  return { dialogServiceInstance, jsonDataServiceInstance };
});

const mockInitialData = {
  initialData: {
    inputPorts: [],
    flowVariables: [],
    language: "Python",
  },
  settings: {
    script: "print('hello world')",
  },
};

describe("init", () => {
  beforeEach(() => {
    // Setup default mock implementations
    jsonDataServiceInstance.initialData.mockResolvedValue(mockInitialData);
    dialogServiceInstance.getInitialDisplayMode.mockReturnValue("large");
  });

  afterEach(() => {
    // Clear all mocks
    vi.restoreAllMocks();

    // Reset display mode
    displayMode.value = "small";
  });

  describe("service creation and assignment", () => {
    it("creates and assigns scripting service", async () => {
      await init();

      const scriptingService = getScriptingService();
      expect(scriptingService).toBeDefined();
      expect(scriptingService).toBeInstanceOf(Object);
    });

    it("creates and assigns settings service", async () => {
      await init();

      const settingsService = getSettingsService();
      expect(settingsService).toBeDefined();
      expect(settingsService).toBeInstanceOf(Object);
    });

    it("sets initial data", async () => {
      await init();

      const initialData = getInitialData();
      expect(initialData).toBeDefined();
      expect(initialData).toBe(mockInitialData.initialData);
    });

    it("makes all services accessible via getters after initialization", async () => {
      await init();

      expect(getScriptingService()).toBeDefined();
      expect(getSettingsService()).toBeDefined();
      expect(getInitialData()).toBeDefined();
    });
  });

  describe("display mode management", () => {
    it("sets initial display mode from dialog service", async () => {
      dialogServiceInstance.getInitialDisplayMode.mockReturnValue("small");

      await init();

      expect(
        dialogServiceInstance.getInitialDisplayMode,
      ).toHaveBeenCalledOnce();
      expect(displayMode.value).toBe("small");
    });

    it("sets initial display mode to large when dialog service returns large", async () => {
      dialogServiceInstance.getInitialDisplayMode.mockReturnValue("large");

      await init();

      expect(displayMode.value).toBe("large");
    });

    it("registers display mode change callback", async () => {
      await init();

      expect(
        dialogServiceInstance.addOnDisplayModeChangeCallback,
      ).toHaveBeenCalledOnce();
      expect(
        typeof dialogServiceInstance.addOnDisplayModeChangeCallback.mock
          .calls[0][0],
      ).toBe("function");
    });

    it("updates display mode when callback is triggered", async () => {
      await init();

      // Get the registered callback
      const callback =
        dialogServiceInstance.addOnDisplayModeChangeCallback.mock.calls[0][0];

      // Set initial state
      displayMode.value = "large";

      // Trigger callback with new mode
      callback({ mode: "small" });

      expect(displayMode.value).toBe("small");
    });

    it("updates display mode from small to large via callback", async () => {
      await init();

      const callback =
        dialogServiceInstance.addOnDisplayModeChangeCallback.mock.calls[0][0];

      // Set initial state
      displayMode.value = "small";

      // Trigger callback with new mode
      callback({ mode: "large" });

      expect(displayMode.value).toBe("large");
    });
  });

  describe("initial data handling", () => {
    it("sets initial data from service", async () => {
      jsonDataServiceInstance.initialData.mockResolvedValue(mockInitialData);

      await init();
      expect(getInitialData()).toBe(mockInitialData.initialData);
    });

    it("passes correct settings to settings service", async () => {
      const customSettings = {
        script: "custom script content",
        someOtherSetting: "test value",
      };

      const customMockData = {
        initialData: {
          inputPorts: [],
          flowVariables: [],
          language: "R",
        },
        settings: customSettings,
      };

      jsonDataServiceInstance.initialData.mockResolvedValue(customMockData);

      await init();

      const settingsService = getSettingsService();
      const retrievedSettings = await settingsService.getSettings();

      expect(retrievedSettings).toEqual(customSettings);
    });
  });
});
