import { type Ref, provide } from "vue";

import type { SettingsIdContext } from "@/nodeDialog/composables/nodeDialog/useUpdates";
import type { Provided } from "@/nodeDialog/types/provided";
import inject from "@/nodeDialog/utils/inject";

export const addSettingsIdStateProviders = (
  settingsId: Ref<SettingsIdContext | null>,
) => {
  const injectionKey = "addStateProviderListener";
  const addStateProviderListener = inject(injectionKey);

  const wrapperWithSettingsId: Provided[typeof injectionKey] = (
    location: Parameters<Provided[typeof injectionKey]>[0],
    callback,
  ) =>
    addStateProviderListener(
      {
        ...location,
        ...(settingsId.value ? { settingsId: settingsId.value } : {}),
      },
      callback,
    );

  provide(injectionKey, wrapperWithSettingsId);
};

export const addSettingsIdToTriggers = (
  settingsId: Ref<SettingsIdContext | null>,
) => {
  const injectionKeyTrigger = "trigger";
  const trigger = inject(injectionKeyTrigger);

  const wrapperWithSettingsIdTrigger: Provided[typeof injectionKeyTrigger] = (
    triggerId: unknown,
  ) => {
    const { id, indexIds = [] } = triggerId as {
      id: string;
      indexIds?: string[];
    };
    trigger({
      id,
      indexIds,
      ...(settingsId.value ? { settingsId: settingsId.value } : {}),
    });
  };

  provide(injectionKeyTrigger, wrapperWithSettingsIdTrigger);

  const injectionKeyIsTriggerActive = "isTriggerActive";
  const isTriggerActive = inject(injectionKeyIsTriggerActive);
  const wrapperWithSettingsIdTriggerIsActive: Provided[typeof injectionKeyIsTriggerActive] =
    ({ id, indexIds = [] }) =>
      isTriggerActive({
        id,
        indexIds,
        ...(settingsId.value ? { settingsId: settingsId.value } : {}),
      });

  provide(injectionKeyIsTriggerActive, wrapperWithSettingsIdTriggerIsActive);
};
