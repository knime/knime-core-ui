import type {
  DialogService,
  JsonDataService,
  SettingComparator,
} from "@knime/ui-extension-service";

import type { PublicAPI } from "./types/public-api";

export type GenericNodeSettings = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
  settingsAreOverriddenByFlowVariable?: boolean;
};

export class SettingsService {
  /**
   * Internal constructor — do not call directly.
   *
   * This constructor is intended for internal use only.
   * Please use the `init` method in `init.ts` to create the singleton instance and
   * access via `getScriptingService()`. Only one instance should be created via the
   * designated initialization method.
   *
   * @internal
   * @param jsonDataService The service used for JSON data operations.
   */
  constructor(
    private readonly initialSettings: GenericNodeSettings,
    private readonly dialogService: DialogService,
    private readonly jsonDataService: JsonDataService,
  ) {}

  getSettings() {
    return this.initialSettings;
  }

  registerSettingsGetterForApply(settingsGetter: () => GenericNodeSettings) {
    this.dialogService.setApplyListener(async () => {
      const settings = settingsGetter();
      try {
        await this.jsonDataService.applyData(settings);
        return { isApplied: true };
      } catch (e) {
        consola.warn("Failed to apply settings", e);
        return { isApplied: false };
      }
    });
  }

  registerSettings(modelOrView: "model" | "view") {
    return <T>(
      initialSetting: T,
      valueComparator?: SettingComparator<T> | undefined,
    ) =>
      this.dialogService.registerSettings(modelOrView)({
        initialValue: initialSetting,
        valueComparator,
      });
  }

  registerSettings2(modelOrView: "model" | "view") {
    return this.dialogService.registerSettings(modelOrView);
  }
}

/** Type representing the public API of SettingsService */
export type SettingsServiceType = PublicAPI<SettingsService>;
