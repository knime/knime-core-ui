<script setup lang="ts">
import { FlowVariableModels } from "@/nodeDialog/composables/components/useFlowVariableModel";
import { ConfigPath } from "@/nodeDialog/composables/components/useFlowVariables";
import TwinlistFlowVariableControl from "./TwinlistFlowVariableControl.vue";

defineProps<{
  configPaths: ConfigPath[];
  flowVariableModel: FlowVariableModels;
}>();
</script>

<template>
  <TwinlistFlowVariableControl
    v-if="flowVariableModel.type === 'twinlist'"
    :flow-variable-model="flowVariableModel.model"
    :config-paths="configPaths"
  />
</template>
