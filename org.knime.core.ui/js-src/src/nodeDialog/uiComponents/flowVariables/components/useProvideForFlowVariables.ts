import { type MaybeRef, computed, provide, unref } from "vue";

import type { FlowSettings } from "@/nodeDialog/api/types";
import {
  type ConfigPath,
  injectionKey,
  toFlowSetting,
} from "@/nodeDialog/composables/components/useFlowVariables";
import { getFlowVariablesMap } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import useDirtySettings, {
  type FlowVariablesForSettings,
} from "@/nodeDialog/composables/nodeDialog/useDirtySettings";

export interface useProvidedFlowVariablesProps {
  dataPath: string;
  configPaths: MaybeRef<ConfigPath[]>;
  flowSettings?: MaybeRef<FlowSettings | null>;
  flowVariablesForSettings?: FlowVariablesForSettings;
}

export const useProvideForFlowVariables = ({
  dataPath,
  configPaths,
  flowSettings: flowSettingsProp,
  flowVariablesForSettings,
}: useProvidedFlowVariablesProps) => {
  const allConfigPaths = unref(configPaths).flatMap(
    ({ configPath, deprecatedConfigPaths }) => [
      configPath,
      ...deprecatedConfigPaths,
    ],
  );
  const dirtySettingsAPI = useDirtySettings();

  const flowVariablesMap = getFlowVariablesMap();
  const getSettingStateFlowVariables = () =>
    flowVariablesForSettings ??
    dirtySettingsAPI.getFlowVariableDirtyState(dataPath) ??
    dirtySettingsAPI.constructFlowVariableDirtyState(
      dataPath,
      allConfigPaths,
      flowVariablesMap,
    );
  const flowSettings = computed(() =>
    flowSettingsProp
      ? unref(flowSettingsProp)
      : toFlowSetting(flowVariablesMap, unref(configPaths)),
  );

  provide(injectionKey, {
    flowSettings,
    configPaths: computed(() => unref(configPaths)),
    getSettingStateFlowVariables,
  });
};
