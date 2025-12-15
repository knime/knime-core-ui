import { DEFAULT_INITIAL_DATA } from "../src/initial-data-service-browser-mock";
import { createScriptingServiceMock } from "../src/scripting-service-browser-mock";
import {
  DEFAULT_INITIAL_SETTINGS,
  createSettingsServiceMock,
} from "../src/settings-service-browser-mock";

import { type AppInitialData } from "./app-initial-data";

const initialData = {
  language: "python",
  fileName: "script.py",
  ...DEFAULT_INITIAL_DATA,
} satisfies AppInitialData;
const scriptingService = createScriptingServiceMock({});
const settingsService = createSettingsServiceMock(DEFAULT_INITIAL_SETTINGS);

export default {
  initialData,
  scriptingService,
  settingsService,
  displayMode: "large" as const,
};
