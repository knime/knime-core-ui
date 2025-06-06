import { computed } from "vue";

import { getFlowVariableSettingsProvidedByControl } from "../../../composables/components/useFlowVariables";
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";

export default (persistPath: string) => {
  const flowVariablesMap = getFlowVariablesMap();

  const { getSettingStateFlowVariables } =
    getFlowVariableSettingsProvidedByControl();
  const {
    exposed: { get: getDirtyExposedVariable },
  } = getSettingStateFlowVariables();
  const flowSettings = computed(() => flowVariablesMap[persistPath]);
  const exposedFlowVariableName = computed(
    () => flowSettings.value?.exposedFlowVariableName ?? "",
  );

  const setExposedVariableState = (
    persistPath: string,
    nonEmptyStringOrNull: string | null,
  ) => {
    const exposedVariableState = getDirtyExposedVariable(persistPath);
    if (nonEmptyStringOrNull === null) {
      exposedVariableState?.unset();
    } else {
      exposedVariableState?.set(nonEmptyStringOrNull);
    }
  };

  const setExposedFlowVariable = ({
    path,
    flowVariableName,
  }: {
    path: string;
    flowVariableName: string;
  }) => {
    const nonEmptyStringOrNull = flowVariableName.trim()
      ? flowVariableName
      : null;
    setExposedVariableState(path, nonEmptyStringOrNull);

    if (
      nonEmptyStringOrNull === null &&
      !flowVariablesMap[path]?.controllingFlowVariableName
    ) {
      // If there is no controlling flow variable, we can remove the flow variable settings
      // for this path.
      delete flowVariablesMap[path];
    } else {
      const flowVarAtPath = flowVariablesMap[path] || {};
      flowVarAtPath.exposedFlowVariableName = nonEmptyStringOrNull;
      flowVariablesMap[path] = flowVarAtPath;
    }
  };
  return { exposedFlowVariableName, setExposedFlowVariable };
};
