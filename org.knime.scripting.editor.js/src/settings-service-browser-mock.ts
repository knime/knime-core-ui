import type {
  GenericNodeSettings,
  SettingsServiceType,
} from "./settings-service";

export const DEFAULT_INITIAL_SETTINGS: GenericNodeSettings = {
  settingsAreOverriddenByFlowVariable: false,
  script: "hello world (from browser mock)",
};

const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};

export const createSettingsServiceMock = (
  data?: GenericNodeSettings,
): SettingsServiceType => ({
  getSettings: () => {
    log("Called settings service mock getSettings");
    return Promise.resolve(data ?? DEFAULT_INITIAL_SETTINGS);
  },
  registerSettingsGetterForApply: () => {
    log("Called settings service mock registerSettingsGetterForApply");
    return Promise.resolve();
  },
});
