import type { SettingState } from "@knime/ui-extension-service";

import { log } from "@s/log";

import type {
  GenericNodeSettings,
  SettingsServiceType,
} from "./settings-service";

export const DEFAULT_INITIAL_SETTINGS: GenericNodeSettings = {
  settingsAreOverriddenByFlowVariable: false,
  script: "hello world (from browser mock)",
};

const setUnsetHelper = (qualifier?: string) => ({
  set: () => {
    log(
      `Called settings service mock registerSettings callback setValue ${qualifier}`,
    );
  },
  unset: () => {
    log(
      `Called settings service mock registerSettings callback unsetValue ${qualifier}`,
    );
  },
});

export const registerSettingsMock = () => {
  log("Called settings service mock registerSettings");
  return () => {
    log("Called settings service mock registerSettings callback");
    const settingState: SettingState = {
      setValue: () => {
        log("Called settings service mock registerSettings callback setValue");
      },
      addExposedFlowVariable: () => {
        log(
          "Called settings service mock registerSettings callback addExposedFlowVariable",
        );
        return setUnsetHelper("addExposedFlowVariable");
      },
      addControllingFlowVariable: () => {
        log(
          "Called settings service mock registerSettings callback addControllingFlowVariable",
        );
        return setUnsetHelper("addControllingFlowVariable");
      },
    };
    return settingState;
  };
};

export const createSettingsServiceMock = (
  data?: GenericNodeSettings,
): SettingsServiceType => ({
  getSettings: () => {
    log("Called settings service mock getSettings");
    return data ?? DEFAULT_INITIAL_SETTINGS;
  },
  registerSettingsGetterForApply: () => {
    log("Called settings service mock registerSettingsGetterForApply");
  },
  registerSettings: registerSettingsMock,
});
