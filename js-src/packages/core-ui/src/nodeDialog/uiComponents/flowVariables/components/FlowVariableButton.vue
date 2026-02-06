<script setup lang="ts">
import { computed, ref } from "vue";

import { KdsPopover, KdsVariableToggleButton } from "@knime/kds-components";

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
const err = computed(() =>
  Boolean(flowSettings.value?.controllingFlowVariableFlawed),
);

const open = ref(false);
const activatorEl = ref<HTMLButtonElement | null>(null);
</script>

<template>
  <template v-if="configPaths.length">
    <KdsVariableToggleButton
      ref="activatorEl"
      v-model="open"
      :in-set="inSet"
      :out-set="outSet"
      :error="err"
      :hidden="!hover && !inSet && !outSet && !err"
    />
    <KdsPopover
      v-model="open"
      :activator-el="activatorEl"
      placement="bottom-left"
      style="width: 380px"
    >
      <FlowVariablePopover
        v-if="open"
        @controlling-flow-variable-set="
          (path, value, flowVarName) =>
            emit('controllingFlowVariableSet', path, value, flowVarName)
        "
      />
    </KdsPopover>
  </template>
</template>
