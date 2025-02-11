import { type Ref, provide } from "vue";

import type { FlowSettings } from "@/nodeDialog/api/types";
import {
  type ConfigPath,
  injectionKey,
} from "@/nodeDialog/composables/components/useFlowVariables";
import { getFlowVariablesMap } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import useDirtySettings from "@/nodeDialog/composables/nodeDialog/useDirtySettings";

export const useProvideForFlowVariables = ({
  dataPath,
  configPaths,
  flowSettings,
}: {
  dataPath: string;
  configPaths: Ref<ConfigPath[]>;
  flowSettings: Ref<FlowSettings | null>;
}) => {
  const allConfigPaths = configPaths.value.flatMap(
    ({ configPath, deprecatedConfigPaths }) => [
      configPath,
      ...deprecatedConfigPaths,
    ],
  );
  const { getFlowVariableDirtyState, constructFlowVariableDirtyState } =
    useDirtySettings();

  const flowVariablesMap = getFlowVariablesMap();
  const getSettingStateFlowVariables = () =>
    getFlowVariableDirtyState(dataPath) ??
    constructFlowVariableDirtyState(dataPath, allConfigPaths, flowVariablesMap);

  provide(injectionKey, {
    flowSettings,
    configPaths,
    getSettingStateFlowVariables,
  });
};
