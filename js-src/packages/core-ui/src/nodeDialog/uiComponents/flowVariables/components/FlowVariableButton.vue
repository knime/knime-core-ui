<script setup lang="ts">
import { computed, ref } from "vue";

import {
  KdsInfoToggleButton,
  KdsPopover,
  KdsVariableToggleButton,
} from "@knime/kds-components";

import { getFlowVariableSettingsProvidedByControl } from "../../../composables/components/useFlowVariables";
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariablePopover from "./FlowVariablePopover.vue";

defineProps<FlowVariableButtonProps>();
const emit = defineEmits<{
  controllingFlowVariableSet: [string, unknown, string];
}>();
const { configPaths } = getFlowVariableSettingsProvidedByControl();

const open = ref(false);
const { flowSettings } = getFlowVariableSettingsProvidedByControl();
const inSet = computed(() =>
  Boolean(flowSettings.value?.controllingFlowVariableName),
);
const outSet = computed(() =>
  Boolean(flowSettings.value?.exposedFlowVariableName),
);
const error = computed(
  () =>
    flowSettings.value?.controllingFlowVariableFlawed ||
    !flowSettings.value?.controllingFlowVariableAvailable,
);
</script>

<template>
  <KdsPopover v-if="configPaths.length" v-model="open">
    <template #activator>
      <KdsVariableToggleButton
        v-if="inSet || outSet"
        :visible="hover"
        :in-set="inSet"
        :out-set="outSet"
        :error="error"
        @click="open = !open"
      />
      <KdsInfoToggleButton
        v-else
        v-model="open"
        :visible="hover"
        icon="flow-variable-default"
        title="No Flow Variable set"
      />
    </template>
    <FlowVariablePopover
      style="width: 380px"
      @controlling-flow-variable-set="
        (path, value, flowVarName) =>
          emit('controllingFlowVariableSet', path, value, flowVarName)
      "
    />
  </KdsPopover>
</template>
