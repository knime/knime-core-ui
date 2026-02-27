import { computed } from "vue";

import type { FlowSettings } from "@/nodeDialog/api/types";
import type { FlowVariablesForSettings } from "@/nodeDialog/composables/nodeDialog/useDirtySettings";
import { getFlowVariableSettingsProvidedByControl } from "../../../composables/components/useFlowVariables";
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";

export const getControllingFlowVariablesMethods = ({
  flowVariablesMap,
  getSettingStateFlowVariables,
}: {
  flowVariablesMap: Record<string, FlowSettings>;
  getSettingStateFlowVariables: () => FlowVariablesForSettings;
}) => {
  const {
    controlling: { get: getDirtyControllingFlowVariable },
  } = getSettingStateFlowVariables();
  const setControllingFlowVariable = ({
    path,
    flowVariableName,
  }: {
    path: string;
    flowVariableName: string;
  }) => {
    const flowVarAtPath = flowVariablesMap[path] || {};
    flowVarAtPath.controllingFlowVariableName = flowVariableName;
    flowVarAtPath.controllingFlowVariableAvailable = true;
    flowVarAtPath.controllingFlowVariableOfCorrectType = true;
    flowVariablesMap[path] = flowVarAtPath;
    getDirtyControllingFlowVariable(path)?.set(flowVariableName);
  };

  const unsetControllingFlowVariable = ({ path }: { path: string }) => {
    if (flowVariablesMap[path]) {
      if (flowVariablesMap[path].exposedFlowVariableName) {
        delete flowVariablesMap[path].controllingFlowVariableFlawed;
        flowVariablesMap[path].controllingFlowVariableAvailable = false;
        flowVariablesMap[path].controllingFlowVariableName = null;
        flowVariablesMap[path].controllingFlowVariableOfCorrectType = false;
      } else {
        delete flowVariablesMap[path];
      }
    }

    getDirtyControllingFlowVariable(path)?.unset();
  };

  const invalidateSetFlowVariable = ({
    path,
    flowVariableName,
  }: {
    path: string;
    flowVariableName: string;
  }) => {
    getDirtyControllingFlowVariable(path)?.set(flowVariableName, {
      isFlawed: true,
    });
  };

  return {
    setControllingFlowVariable,
    unsetControllingFlowVariable,
    invalidateSetFlowVariable,
  };
};

export default (persistPath: string) => {
  const flowVariablesMap = getFlowVariablesMap();

  const { getSettingStateFlowVariables } =
    getFlowVariableSettingsProvidedByControl();
  const flowSettings = computed(() => flowVariablesMap[persistPath]);
  const controllingFlowVariableName = computed(
    () => flowSettings.value?.controllingFlowVariableName ?? "",
  );

  const {
    setControllingFlowVariable,
    unsetControllingFlowVariable,
    invalidateSetFlowVariable,
  } = getControllingFlowVariablesMethods({
    flowVariablesMap,
    getSettingStateFlowVariables,
  });

  return {
    controllingFlowVariableName,
    setControllingFlowVariable,
    unsetControllingFlowVariable,
    invalidateSetFlowVariable,
  };
};
