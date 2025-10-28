import type {
  SettingsUpdate2,
  SettingsUpdate2WithSettingsId,
} from "../composables/nodeDialog/useUpdates";

/**
 * Settings Validation Methods
 */

export type SettingsPerformExternalValidation = (params: {
  method: "settings.performExternalValidation";
  options: [
    /**
     * The ID of the setting to validate
     */
    string,
    /**
     * The value to validate
     */
    any,
  ];
}) => Promise<{ result: string | null }>;

export type SettingsPerformCustomValidation = (params: {
  method: "settings.performCustomValidation";
  options: [
    /**
     * The ID of the setting to validate
     */
    string,
    /**
     * The value to validate
     */
    any,
  ];
}) => Promise<{ result: string | null }>;

/**
 * Combined Settings API type
 */
export type SettingsRpcMethods = SettingsUpdate2 &
  SettingsUpdate2WithSettingsId &
  SettingsPerformExternalValidation &
  SettingsPerformCustomValidation;
