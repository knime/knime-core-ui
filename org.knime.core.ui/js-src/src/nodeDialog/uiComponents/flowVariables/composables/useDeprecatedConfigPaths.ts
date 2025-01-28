import { computed } from "vue";

import { getFlowVariableSettingsProvidedByControl } from "../../../composables/components/useFlowVariables";
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";

export default () => {
  const { configPaths } = getFlowVariableSettingsProvidedByControl();

  const flowVariablesMap = getFlowVariablesMap();

  const getFlowVariablePathsStartingWithKey = (key: string) =>
    Object.keys(flowVariablesMap).filter((flowVariablePath) =>
      flowVariablePath.startsWith(key),
    );

  const deprecatedConfigPaths = computed(() => {
    return configPaths.value.flatMap(
      ({ deprecatedConfigPaths }) => deprecatedConfigPaths,
    );
  });

  const deprecatedSetConfigPaths = computed(() =>
    deprecatedConfigPaths.value.flatMap(getFlowVariablePathsStartingWithKey),
  );

  return { deprecatedSetConfigPaths };
};
