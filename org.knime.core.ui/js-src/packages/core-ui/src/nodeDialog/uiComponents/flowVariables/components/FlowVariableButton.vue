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

const isMissing = computed(
  () =>
    Boolean(flowSettings.value?.controllingFlowVariableName) &&
    flowSettings.value?.controllingFlowVariableAvailable === false,
);
const isWrongType = computed(
  () =>
    Boolean(flowSettings.value?.controllingFlowVariableName) &&
    flowSettings.value?.controllingFlowVariableOfCorrectType === false,
);
const error = computed(() => isMissing.value || isWrongType.value);
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
