import { computed } from "vue";

import {
  getControlledOrExposedPathsStartingWith,
  getFlowVariableSettingsProvidedByControl,
} from "../../../composables/components/useFlowVariables";
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";

export default () => {
  const { configPaths } = getFlowVariableSettingsProvidedByControl();

  const flowVariablesMap = getFlowVariablesMap();
  const getPathsStartingWith =
    getControlledOrExposedPathsStartingWith(flowVariablesMap);

  const deprecatedConfigPaths = computed(() => {
    const deprecatedConfigPathsWithDuplicates = configPaths.value.flatMap(
      ({ deprecatedConfigPaths }) => deprecatedConfigPaths,
    );
    return [...new Set(deprecatedConfigPathsWithDuplicates)];
  });

  const deprecatedSetConfigPaths = computed(() => {
    const deprecatedSetConfigPathsWithDuplicates =
      deprecatedConfigPaths.value.flatMap(getPathsStartingWith);
    return [...new Set(deprecatedSetConfigPathsWithDuplicates)];
  });

  return { deprecatedSetConfigPaths };
};
