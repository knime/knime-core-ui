import {
  DialogService,
  JsonDataService,
  type SettingState,
} from "@knime/ui-extension-service";

import type { InputOutputModel } from "@/components/InputOutputItem.vue";

import { displayMode } from "./display-mode";
import {
  ScriptingService,
  type ScriptingServiceType,
} from "./scripting-service";

// --- TYPES ---

export type PortViewConfig = {
  label: string;
  portViewIdx: number;
};

export type PortConfig = {
  /**
   * null if no node is connected to an input port
   */
  nodeId: string | null;
  portIdx: number;
  portViewConfigs: PortViewConfig[];
  portName: string;
};

export type PortConfigs = {
  inputPorts: PortConfig[];
};

export type ConnectionStatus =
  /** The input is not connected */
  | "MISSING_CONNECTION"
  /** The input is connected, but the predecessor is not configured */
  | "UNCONFIGURED_CONNECTION"
  /** The input is connected and configured, but the predecessor is not executed */
  | "UNEXECUTED_CONNECTION"
  /** The input is connected, configured, and executed */
  | "OK";

export type InputConnectionInfo = {
  status: ConnectionStatus;
  isOptional: boolean;
};

export type KAIConfig = {
  hubId: string;
  isKaiEnabled: boolean;
};

export type GenericInitialData = {
  inputPortConfigs: PortConfigs;
  inputObjects: InputOutputModel[];
  flowVariables: InputOutputModel;
  inputConnectionInfo: InputConnectionInfo[];
  outputObjects?: InputOutputModel[];
  kAiConfig: KAIConfig;
};

export type GenericNodeSettings = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
  settingsAreOverriddenByFlowVariable?: boolean;
};

type InitialDataAndSettings = {
  initialData: GenericInitialData;
  settings: GenericNodeSettings;
};

export type InitialDataServiceType = {
  getInitialData: () => Promise<GenericInitialData>;
};

export type SettingsServiceType = {
  getSettings: () => Promise<GenericNodeSettings>;
  registerSettingsGetterForApply: (
    settingsGetter: () => GenericNodeSettings,
  ) => Promise<void>;
  registerSettings: <T>(
    modelOrView: "model" | "view",
  ) => Promise<(initialSetting: T) => SettingState<T>>;
};

// --- INSTANCES ---

let scriptingService: ScriptingServiceType;
export const getScriptingService = (): ScriptingServiceType => scriptingService;

export let initialDataService: InitialDataServiceType;
export let settingsService: SettingsServiceType;

// --- INIT FUNCTION ---

// TODO jsdoc
export const init = async () => {
  const jsonDataService = await JsonDataService.getInstance();
  const dialogService = await DialogService.getInstance();

  scriptingService = new ScriptingService(jsonDataService);

  const initialDataAndSettings: InitialDataAndSettings =
    await jsonDataService.initialData();

  initialDataService = {
    // TODO this can now return the initial data, not a promise
    getInitialData: () => Promise.resolve(initialDataAndSettings.initialData),
  };

  settingsService = {
    getSettings: () => Promise.resolve(initialDataAndSettings.settings),
    registerSettingsGetterForApply: (
      settingsGetter: () => GenericNodeSettings,
    ) => {
      dialogService.setApplyListener(async () => {
        const settings = settingsGetter();
        try {
          await jsonDataService.applyData(settings);
          return { isApplied: true };
        } catch (e) {
          consola.warn("Failed to apply settings", e);
          return { isApplied: false };
        }
      });
      // TODO this does not have to be async anymore
      return Promise.resolve();
    },
    registerSettings: (modelOrView: "model" | "view") => {
      // TODO this does not have to be async anymore
      return Promise.resolve(<T>(initialSetting: T) =>
        dialogService.registerSettings(modelOrView)({
          initialValue: initialSetting,
        }),
      );
    },
  };

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
