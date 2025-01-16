<script setup lang="ts">
import { type Ref, provide } from "vue";

import type { FlowSettings } from "../../../api/types";
import {
  type ConfigPath,
  injectionKey,
} from "../../../composables/components/useFlowVariables";
import { getFlowVariablesMap } from "../../../composables/components/useProvidedFlowVariablesMap";
import useDirtySettings from "../../../composables/nodeDialog/useDirtySettings";
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariableButton from "./FlowVariableButton.vue";

const props = defineProps<
  FlowVariableButtonProps & {
    dataPath: string;
    configPaths: Ref<ConfigPath[]>;
    flowSettings: Ref<FlowSettings | null>;
  }
>();

const emit = defineEmits<{
  controllingFlowVariableSet: [string, unknown, string];
}>();
const allConfigPaths = props.configPaths.value.flatMap(
  ({ configPath, deprecatedConfigPaths }) => [
    configPath,
    ...deprecatedConfigPaths,
  ],
);

const { getFlowVariableDirtyState, constructFlowVariableDirtyState } =
  useDirtySettings();

const flowVariablesMap = getFlowVariablesMap();
const getSettingStateFlowVariables = () =>
  getFlowVariableDirtyState(props.dataPath) ??
  constructFlowVariableDirtyState(
    props.dataPath,
    allConfigPaths,
    flowVariablesMap,
  );

provide(injectionKey, {
  flowSettings: props.flowSettings,
  configPaths: props.configPaths,
  getSettingStateFlowVariables,
});
</script>

<template>
  <FlowVariableButton
    :hover="hover"
    @controlling-flow-variable-set="
      (path, value, flowVarName) =>
        emit('controllingFlowVariableSet', path, value, flowVarName)
    "
  />
</template>
