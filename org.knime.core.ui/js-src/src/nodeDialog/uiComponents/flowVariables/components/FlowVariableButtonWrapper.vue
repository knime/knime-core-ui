<script setup lang="ts">
import { type Ref } from "vue";

import type { FlowSettings } from "../../../api/types";
import { type ConfigPath } from "../../../composables/components/useFlowVariables";
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariableButton from "./FlowVariableButton.vue";
import { useProvideForFlowVariables } from "./useProvideForFlowVariables";

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
