import {
  type GenericNodeSettings,
  type SettingsServiceType,
  settingsService,
} from "./init";

export type { GenericNodeSettings, SettingsServiceType };

export const getSettingsService = (): SettingsServiceType => settingsService;
