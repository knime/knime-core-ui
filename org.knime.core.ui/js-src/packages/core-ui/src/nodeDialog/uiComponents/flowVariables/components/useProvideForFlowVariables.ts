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

export interface UseProvidedFlowVariablesProps {
  dataPath: string;
  configPaths: MaybeRef<ConfigPath[]>;
  /**
   * If not provided, this is computed from the configPaths and flow variables map.
   */
  flowSettings?: MaybeRef<FlowSettings | null>;
  /**
   * Provide this prop if this component is not used within a wrapper that provides
   * methods to retrieve it automatically.
   * It is obtained by the result of `registerSettings` and used to involve flow variables
   * in dirty state tracking.
   */
  flowVariablesForSettings?: FlowVariablesForSettings;
}

export const useProvideForFlowVariables = ({
  dataPath,
  configPaths,
  flowSettings: flowSettingsProp,
  flowVariablesForSettings,
}: UseProvidedFlowVariablesProps) => {
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
