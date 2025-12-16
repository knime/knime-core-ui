import type { FlowSettings } from "@knime/core-ui/src/nodeDialog/api/types";
import type { NodeDialogCoreInitialData } from "@knime/core-ui/src/nodeDialog/types/InitialData";
import type {
  DialogService,
  JsonDataService,
} from "@knime/ui-extension-service";

import type { PublicAPI } from "./types/public-api";

export type GenericNodeSettings = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
  settingsAreOverriddenByFlowVariable?: boolean;
};

export type SettingsInitialData = NodeDialogCoreInitialData & {
  flowVariableSettings: Record<string, FlowSettings>;
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
   * @param initialSettings Initial settings from `ScriptingNodeSettingsService`, or undefined if `settingsInitialData` is provided
   * @param settingsInitialData Initial settings data from `ScriptingDefaultNodeSettingsService`, or undefined if `initialSettings` is provided
   * @param dialogService The service for dialog operations
   * @param jsonDataService The service for JSON data operations
   */
  constructor(
    private readonly initialSettings: GenericNodeSettings | undefined,
    private readonly settingsInitialData: SettingsInitialData | undefined,
    private readonly dialogService: DialogService,
    private readonly jsonDataService: JsonDataService,
  ) {
    if (initialSettings === undefined && settingsInitialData === undefined) {
      throw new Error(
        "Either initialSettings or settingsInitialData must be provided",
      );
    }
    if (initialSettings !== undefined && settingsInitialData !== undefined) {
      throw new Error(
        "Cannot provide both initialSettings and settingsInitialData",
      );
    }
  }

  getSettings() {
    return this.initialSettings;
  }

  getSettingsInitialData() {
    return this.settingsInitialData;
  }

  registerSettingsGetterForApply(settingsGetter: () => unknown) {
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
    return this.dialogService.registerSettings(modelOrView);
  }
}

/** Type representing the public API of SettingsService */
export type SettingsServiceType = PublicAPI<SettingsService>;
