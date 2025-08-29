// Mock the UI extension service BEFORE any other imports
import { DialogService, JsonDataService } from "@knime/ui-extension-service";

// Mock JsonDataService to prevent backend connection attempts
const mockJsonDataService = {
  sendRequest(method: string, params: any[] = []) {
    // eslint-disable-next-line no-console
    console.log(`Mock JsonDataService.sendRequest: ${method}`, params);

    // Return appropriate responses for different methods
    switch (method) {
      case "getConfig":
        return {};
      case "getInitialData":
        return {};
      default:
        return {};
    }
  },

  async initialData() {
    await new Promise((resolve) => setTimeout(resolve, 500));
  },

  registerSettingsHandler() {
    // eslint-disable-next-line no-console
    console.log("Mock JsonDataService.registerSettingsHandler");
    return () => {};
  },

  registerEventHandler() {
    // eslint-disable-next-line no-console
    console.log("Mock JsonDataService.registerEventHandler");
    return () => {};
  },
};

// Override the getInstance method
JsonDataService.getInstance = () => Promise.resolve(mockJsonDataService as any);

// Mock DialogService for display mode
const mockDialogService = {
  getInitialDisplayMode() {
    return "large" as const;
  },

  addOnDisplayModeChangeCallback() {
    // eslint-disable-next-line no-console
    console.log("Mock DialogService.addOnDisplayModeChangeCallback");
    return () => {};
  },
};

DialogService.getInstance = () => Promise.resolve(mockDialogService as any);

// Now import and setup the scripting editor services
import { getInitialDataService } from "../src/initial-data-service";
import { createInitialDataServiceMock } from "../src/initial-data-service-browser-mock";
import { getScriptingService } from "../src/scripting-service";
import { createScriptingServiceMock } from "../src/scripting-service-browser-mock";
import { getSettingsService } from "../src/settings-service";
import { createSettingsServiceMock } from "../src/settings-service-browser-mock";

import { DEFAULT_INITIAL_DATA, DEFAULT_INITIAL_SETTINGS } from "./mock-data";

// Always use mocks in demo mode
const scriptingService = createScriptingServiceMock({
  sendToServiceMockResponses: {
    getLanguageServerConfig: () => Promise.resolve(JSON.stringify({})),
    runScript: () => {
      // eslint-disable-next-line no-console
      console.log("Mock: Running script...");
      // Simulate some console output
      const consoleHandler = scriptingService.eventHandlers.get("console");
      if (consoleHandler) {
        consoleHandler({
          text: "Mock output: Script executed successfully!\n",
          type: "stdout",
        });
      }
      return Promise.resolve();
    },
    runSelectedLines: () => {
      // eslint-disable-next-line no-console
      console.log("Mock: Running selected lines...");
      const consoleHandler = scriptingService.eventHandlers.get("console");
      if (consoleHandler) {
        consoleHandler({
          text: "Mock output: Selected lines executed!\n",
          type: "stdout",
        });
      }
      return Promise.resolve();
    },
  },
});

const initialDataService = createInitialDataServiceMock(DEFAULT_INITIAL_DATA);
const settingsService = createSettingsServiceMock(DEFAULT_INITIAL_SETTINGS);

Object.assign(getInitialDataService(), initialDataService);
Object.assign(getScriptingService(), scriptingService);
Object.assign(getSettingsService(), settingsService);
