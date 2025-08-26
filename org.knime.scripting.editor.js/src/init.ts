import { DialogService, JsonDataService } from "@knime/ui-extension-service";

import { displayMode } from "./display-mode";
import type {
  GenericInitialData,
  InitialDataServiceType,
} from "./initial-data-service";
import {
  ScriptingService,
  type ScriptingServiceType,
} from "./scripting-service";
import {
  type GenericNodeSettings,
  SettingsService,
  type SettingsServiceType,
} from "./settings-service";

// --- INSTANCES ---

let scriptingService: ScriptingServiceType;
export const getScriptingService = (): ScriptingServiceType => scriptingService;

let initialDataService: InitialDataServiceType;
export const getInitialDataService = (): InitialDataServiceType =>
  initialDataService;

let settingsService: SettingsServiceType;
export const getSettingsService = (): SettingsServiceType => settingsService;

// --- INIT FUNCTION ---

// TODO jsdoc
export const init = async () => {
  const jsonDataService = await JsonDataService.getInstance();
  const dialogService = await DialogService.getInstance();

  scriptingService = new ScriptingService(jsonDataService);

  const initialDataAndSettings: {
    initialData: GenericInitialData;
    settings: GenericNodeSettings;
  } = await jsonDataService.initialData();

  initialDataService = {
    // TODO this can now return the initial data, not a promise
    getInitialData: () => Promise.resolve(initialDataAndSettings.initialData),
  };

  settingsService = new SettingsService(
    initialDataAndSettings.settings,
    dialogService,
    jsonDataService,
  );

  // Set the initial value of displayMode
  displayMode.value = dialogService.getInitialDisplayMode();

  // Register a listener to update displayMode whenever it changes
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });
};

// Alternative that uses a mock
export type InitMockData = {
  scriptingService: ScriptingServiceType;
  initialDataService: InitialDataServiceType;
  settingsService: SettingsServiceType;
  displayMode: "small" | "large";
};

export const initMocked = (mockData: InitMockData) => {
  scriptingService = mockData.scriptingService;
  initialDataService = mockData.initialDataService;
  settingsService = mockData.settingsService;
  displayMode.value = mockData.displayMode;
};
