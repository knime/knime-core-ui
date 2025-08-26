/* eslint-disable @typescript-eslint/no-explicit-any */
import {
  DialogService,
  JsonDataService,
  type SettingState,
} from "@knime/ui-extension-service";

import type { InputOutputModel } from "@/components/InputOutputItem.vue";

import { displayMode } from "./display-mode";
import { MonacoLSPConnection } from "./lsp/connection";
import { ScriptingService } from "./scripting-service";

// --- TYPES ---

type LanguageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

export type UsageData = {
  limit: number | null;
  used: number;
};

export type ScriptingServiceType = {
  sendToService(methodName: string, options?: any[] | undefined): Promise<any>;
  registerEventHandler(type: string, handler: (args: any) => void): void;
  connectToLanguageServer(): Promise<MonacoLSPConnection>;
  isCallKnimeUiApiAvailable(portToTestFor: PortConfig): Promise<boolean>;
  isKaiEnabled(): Promise<boolean>;
  isLoggedIntoHub(): Promise<boolean>;
  getAiDisclaimer(): Promise<string>;
  getAiUsage(): Promise<UsageData | null>;
};

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
  registerSettings: (
    modelOrView: "model" | "view",
  ) => Promise<(initialSetting: unknown) => SettingState<unknown>>;
};

// --- HELPER CLASSES ---

// /// MOVED FROM settings-helper.ts

class SettingsHelper {
  private static instance: SettingsHelper;
  private jsonDataService: Promise<JsonDataService>;
  private readonly dialogService: Promise<DialogService>;

  private cachedInitialDataAndSettings: InitialDataAndSettings | null = null;

  private constructor() {
    this.jsonDataService = JsonDataService.getInstance();
    this.dialogService = DialogService.getInstance();
  }

  private async loadDataIntoCache(): Promise<void> {
    this.cachedInitialDataAndSettings = (await (
      await this.jsonDataService
    ).initialData()) as InitialDataAndSettings;
  }

  public async getInitialDataAndSettings(): Promise<InitialDataAndSettings> {
    if (!this.cachedInitialDataAndSettings) {
      await this.loadDataIntoCache();
    }

    return this.cachedInitialDataAndSettings!;
  }

  public async registerApplyListener(
    settingsGetter: () => GenericNodeSettings,
  ): Promise<void> {
    const dialogService = await this.dialogService;
    dialogService.setApplyListener(async () => {
      const settings = settingsGetter();
      try {
        await (await this.jsonDataService).applyData(settings);
        return { isApplied: true };
      } catch (e) {
        consola.warn("Failed to apply settings", e);
        return { isApplied: false };
      }
    });
  }

  public async registerSettings<T>(
    modelOrView: "view" | "model",
  ): Promise<(initialSetting: T) => SettingState<T>> {
    const dialogService = await this.dialogService;

    return (initialSetting: T) =>
      dialogService.registerSettings(modelOrView)({
        initialValue: initialSetting,
      });
  }

  public static getInstance(): SettingsHelper {
    if (!SettingsHelper.instance) {
      SettingsHelper.instance = new SettingsHelper();
    }
    return SettingsHelper.instance;
  }
}

// --- INSTANCES ---

export let scriptingService: ScriptingServiceType;
export let initialDataService: InitialDataServiceType;
export let settingsService: SettingsServiceType;

// --- INIT FUNCTION ---

// TODO jsdoc
export const init = async () => {
  const jsonDataService = await JsonDataService.getInstance();

  scriptingService = new ScriptingService(jsonDataService);

  const initialDataAndSettings =
    await SettingsHelper.getInstance().getInitialDataAndSettings();

  initialDataService = {
    // TODO this can now return the initial data, not a promise
    getInitialData: () => Promise.resolve(initialDataAndSettings.initialData),
  };

  settingsService = {
    getSettings: () => Promise.resolve(initialDataAndSettings.settings),
    registerSettingsGetterForApply: (
      settingsGetter: () => GenericNodeSettings,
    ) => SettingsHelper.getInstance().registerApplyListener(settingsGetter),
    registerSettings: (modelOrView: "model" | "view") =>
      SettingsHelper.getInstance().registerSettings(modelOrView),
  };

  // Set the display mode
  await DialogService.getInstance().then((dialogService) => {
    // Set the initial value of displayMode
    displayMode.value = dialogService.getInitialDisplayMode();

    // Register a listener to update displayMode whenever it changes
    dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
      displayMode.value = mode;
    });
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
