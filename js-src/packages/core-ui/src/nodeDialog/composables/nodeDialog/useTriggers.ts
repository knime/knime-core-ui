import type { Result } from "../../api/types/Result";

import type { DialogSettings, SettingsIdContext } from "./useUpdates";

export type IndexedIsActive = { indices: string[]; isActive: boolean };
export type IsActiveCallback = (
  indexIds: string[],
) => (settings: DialogSettings) => Promise<Result<IndexedIsActive[]>>;

export type TriggerCallback = (
  indexIds: string[],
) => (
  dependencySettings: DialogSettings,
) => Promise<(newSettings: DialogSettings) => void>;

export default () => {
  const registeredTriggers = new Map<string, TriggerCallback>();

  const registeredTriggersActive = new Map<string, IsActiveCallback>();

  const getKey = (
    settingsId: SettingsIdContext | undefined,
    triggerId: string,
  ) => {
    return settingsId
      ? `dynamicsettings(${settingsId.settingsId}):atPath(${settingsId.currentParentPathSegments}):${triggerId}`
      : triggerId;
  };

  const registerTrigger = (
    triggerId: string,
    isActive: IsActiveCallback,
    callback: TriggerCallback,
    settingsId?: SettingsIdContext,
  ) => {
    const key = getKey(settingsId, triggerId);
    registeredTriggers.set(key, callback);
    registeredTriggersActive.set(key, isActive);
  };

  const getTriggerCallback = ({
    id,
    indexIds,
    settingsId,
  }: {
    id: string;
    indexIds?: string[];
    settingsId?: SettingsIdContext;
  }) => {
    const key = getKey(settingsId, id);
    const callback = registeredTriggers.get(key);
    if (!callback) {
      throw new Error(`No trigger registered for id ${key}`);
    }
    return callback(indexIds ?? []);
  };

  const getTriggerIsActiveCallback = ({
    id,
    indexIds,
    settingsId,
  }: {
    id: string;
    indexIds?: string[];
    settingsId?: SettingsIdContext;
  }) => {
    const key = getKey(settingsId, id);
    const callback = registeredTriggersActive.get(key);
    if (!callback) {
      throw new Error(`No trigger registered for id ${key}`);
    }
    return callback(indexIds ?? []);
  };

  return { registerTrigger, getTriggerCallback, getTriggerIsActiveCallback };
};
