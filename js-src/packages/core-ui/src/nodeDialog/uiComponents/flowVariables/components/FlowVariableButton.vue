<script setup lang="ts">
import { computed, ref } from "vue";

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
const error = computed(
  () => Boolean(flowSettings.value?.controllingFlowVariableFlawed),
  // what about invalid values of flow variables?
);

const open = ref(false);
</script>

<template>
  <KdsVariableToggleButton
    v-if="configPaths.length"
    v-model="open"
    :in-set="inSet"
    :out-set="outSet"
    :error="error"
    :hidden="!hover && !inSet && !outSet && !error"
  >
    <FlowVariablePopover
      v-if="open"
      @controlling-flow-variable-set="
        (path, value, flowVarName) =>
          emit('controllingFlowVariableSet', path, value, flowVarName)
      "
    />
  </KdsVariableToggleButton>
</template>
