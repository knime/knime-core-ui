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
   * @param initialSettings Initial settings from `ScriptingNodeSettingsService`. May be undefined
   *   when using `ScriptingDefaultNodeSettingsService` (JSONForms-based dialogs) or when no initial
   *   settings are available.
   * @param settingsInitialData Initial settings data from `ScriptingDefaultNodeSettingsService`
   *   (includes schema, UI schema, and flow variable settings for JSONForms-based dialogs). May be
   *   undefined when using legacy `ScriptingNodeSettingsService` or when no initial data is available.
   * @param dialogService The service for dialog operations
   * @param jsonDataService The service for JSON data operations
   *
   * @remarks
   * Both `initialSettings` and `settingsInitialData` are optional to support different dialog modes:
   * - Legacy scripting nodes provide only `initialSettings`
   * - JSONForms-based scripting nodes provide only `settingsInitialData`
   * - During initialization, both may be undefined if no data is available yet
   * - Some implementations may provide both for compatibility during migration
   *
   * The service gracefully handles all cases, returning `undefined` from `getSettings()` or
   * `getSettingsInitialData()` when the respective parameter was not provided.
   */
  constructor(
    private readonly initialSettings: GenericNodeSettings | undefined,
    private readonly settingsInitialData: SettingsInitialData | undefined,
    private readonly dialogService: DialogService,
    private readonly jsonDataService: JsonDataService,
  ) {}

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
