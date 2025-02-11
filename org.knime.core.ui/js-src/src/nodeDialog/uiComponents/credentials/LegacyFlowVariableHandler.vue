<script setup lang="ts">
/** This component does not have a template, yes, but it needs to be a
 * subcomponent instead of a composable because it relies on injected data from
 * the parent component.
 */
import { onMounted } from "vue";

import { getFlowVariablesMap } from "@/nodeDialog/composables/components/useProvidedFlowVariablesMap";
import { getFlowVariableSettingsProvidedByControl } from "../../composables/components/useFlowVariables";
import { injectForFlowVariables } from "../../utils/inject";
import { getControllingFlowVariablesMethods } from "../flowVariables/composables/useControllingFlowVariable";

import type { Credentials } from "./types/Credentials";

const { configPaths } = getFlowVariableSettingsProvidedByControl();

const flowVariablesMap = getFlowVariablesMap();

const { getSettingStateFlowVariables } =
  getFlowVariableSettingsProvidedByControl();

const { getFlowVariableOverrideValue } =
  injectForFlowVariables("flowVariablesApi");

const props = defineProps<{
  flowVariableName: string | null | undefined;
}>();
const emit = defineEmits<{
  flowVariableSet: [Credentials | undefined, string];
}>();

onMounted(() => {
  const flowVariableName = props.flowVariableName;
  if (flowVariableName) {
    // timeout required for the setting state to be initialized
    setTimeout(async () => {
      const { setControllingFlowVariable, invalidateSetFlowVariable } =
        getControllingFlowVariablesMethods({
          flowVariablesMap,
          getSettingStateFlowVariables,
        });
      const { configPath: persistPath, dataPath } = configPaths.value[0];
      const setControllingVariableProps = {
        path: configPaths.value[0].configPath,
        flowVariableName,
      };
      setControllingFlowVariable(setControllingVariableProps);
      const value = (await getFlowVariableOverrideValue(
        persistPath,
        dataPath,
      )) satisfies Credentials | undefined;
      if (typeof value === "undefined") {
        invalidateSetFlowVariable(setControllingVariableProps);
      }
      emit("flowVariableSet", value, flowVariableName);
    }, 0);
  }
});
</script>
