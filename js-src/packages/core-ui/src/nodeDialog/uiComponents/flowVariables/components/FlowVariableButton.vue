<script setup lang="ts">
import { computed } from "vue";

import { KdsVariableToggleButton } from "@knime/kds-components";

import { getFlowVariableSettingsProvidedByControl } from "../../../composables/components/useFlowVariables";
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariablePopover from "./FlowVariablePopover.vue";

defineProps<FlowVariableButtonProps>();
const emit = defineEmits<{
  controllingFlowVariableSet: [string, unknown, string];
}>();

const { configPaths, flowSettings } =
  getFlowVariableSettingsProvidedByControl();
const inSet = computed(() =>
  Boolean(flowSettings.value?.controllingFlowVariableName),
);
const outSet = computed(() =>
  Boolean(flowSettings.value?.exposedFlowVariableName),
);

// TODO UIEXT-3393: set error state based on the actual flaw, e.g. name conflict with existing flow variable, invalid variable name, etc.
const error = computed(() => false);
</script>

<template>
  <KdsVariableToggleButton
    v-if="configPaths.length"
    :in-set="inSet"
    :out-set="outSet"
    :error="error"
    :hidden="!hover && !inSet && !outSet && !error"
  >
    <FlowVariablePopover
      @controlling-flow-variable-set="
        (path, value, flowVarName) =>
          emit('controllingFlowVariableSet', path, value, flowVarName)
      "
    />
  </KdsVariableToggleButton>
</template>
