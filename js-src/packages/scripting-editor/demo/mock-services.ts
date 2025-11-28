import { createScriptingServiceMock } from "../src/scripting-service-browser-mock";
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
  unlicensedKaiUser: false,
});

const settingsService = createSettingsServiceMock(DEFAULT_INITIAL_SETTINGS);

export default {
  scriptingService,
  initialData: DEFAULT_INITIAL_DATA,
  settingsService,
  displayMode: "large" as const,
};
