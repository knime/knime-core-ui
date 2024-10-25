import {
  DialogService,
  JsonDataService,
  type SettingState,
} from "@knime/ui-extension-service";

import type { GenericInitialData } from "./initial-data-service";
import type { GenericNodeSettings } from "./settings-service";

type InitialDataAndSettings = {
  initialData: GenericInitialData;
  settings: GenericNodeSettings;
};

class SettingsHelper {
  // eslint-disable-next-line no-use-before-define
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
  ): Promise<(initialSetting: T) => SettingState> {
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

export const getSettingsHelper = (): SettingsHelper =>
  SettingsHelper.getInstance();
