import { ref } from "vue";

import { getSettingsHelper } from "@/settings-helper";

export type GenericNodeSettings = {
  [key: string]: any;
  settingsAreOverriddenByFlowVariable?: boolean;
};

const settingsLoaded = ref(false);

const loadDataPromise = getSettingsHelper()
  .getInitialDataAndSettings()
  .then((data): GenericNodeSettings => {
    settingsLoaded.value = true;
    return data.settings;
  });

const settingsService = {
  getSettings: () => loadDataPromise,
  registerSettingsGetterForApply: (settingsGetter: () => GenericNodeSettings) =>
    getSettingsHelper().registerApplyListener(settingsGetter),
  registerSettings: (modelOrView: "model" | "view") =>
    getSettingsHelper().registerSettings(modelOrView),
};
export type SettingsServiceType = typeof settingsService;

export const getSettingsService = (): SettingsServiceType => settingsService;
