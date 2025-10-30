import {
  AlertingService,
  DialogService,
  JsonDataService,
} from "@knime/ui-extension-service";

import { consoleHandler } from "./consoleHandler";
import { displayMode } from "./display-mode";
import type { GenericInitialData } from "./initial-data-service";
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

let initialData: GenericInitialData;
export const getInitialData = (): GenericInitialData => initialData;

let settingsService: SettingsServiceType;
export const getSettingsService = (): SettingsServiceType => settingsService;

// --- INIT FUNCTION ---

/**
 * Initializes the scripting editor services and sets up the display mode.
 *
 * This function must be called before using any other scripting editor functionality.
 * It creates and configures the singleton instances of ScriptingService and SettingsService,
 * fetches initial data from the backend, and sets up display mode management.
 *
 * @returns A Promise that resolves when all services are initialized
 */
export const init = async () => {
  const jsonDataService = await JsonDataService.getInstance();
  const dialogService = await DialogService.getInstance();
  const alertingService = await AlertingService.getInstance();

  scriptingService = new ScriptingService(jsonDataService, alertingService);

  const initialDataAndSettings: {
    initialData: GenericInitialData;
    settings: GenericNodeSettings;
  } = await jsonDataService.initialData();

  initialData = initialDataAndSettings.initialData;

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
  scriptingService?: ScriptingServiceType;
  initialData?: GenericInitialData;
  settingsService?: SettingsServiceType;
  displayMode?: "small" | "large";
};

export const initMocked = (mockData: InitMockData) => {
  if (mockData.scriptingService) {
    scriptingService = mockData.scriptingService;
  }
  if (mockData.initialData) {
    initialData = mockData.initialData;
  }
  if (mockData.settingsService) {
    settingsService = mockData.settingsService;
  }
  if (mockData.displayMode) {
    displayMode.value = mockData.displayMode;
  }
};

/**
 * Initialize the console event handler to forward console events from the
 * backend to the frontend console handler.
 */
export const initConsoleEventHandler = () => {
  getScriptingService().registerEventHandler("console", consoleHandler.write);
};
