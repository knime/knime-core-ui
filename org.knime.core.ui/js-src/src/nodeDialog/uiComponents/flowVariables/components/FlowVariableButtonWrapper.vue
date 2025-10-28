<script lang="ts">
/**
 * This component can be used to show a flow variable button as it is used in the NodeDialog.
 * Next to its props, it requires two things to be provided via Vue's provide/inject mechanism:
 * 1. The flow variables map
 * 2. The methods to interact with the backend for flow variables
 *
 * It in turn also provides further context for its child components to involve
 * flow variables in dirty state tracking.
 */
export default {};
</script>

<script setup lang="ts">
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariableButton from "./FlowVariableButton.vue";
import {
  type UseProvidedFlowVariablesProps,
  useProvideForFlowVariables,
} from "./useProvideForFlowVariables";

const props = defineProps<
  FlowVariableButtonProps & UseProvidedFlowVariablesProps
>();

const emit = defineEmits<{
  controllingFlowVariableSet: [string, unknown, string];
}>();

useProvideForFlowVariables(props);
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
