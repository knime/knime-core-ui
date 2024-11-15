<script setup lang="ts">
import { getFlowVariableSettingsProvidedByControl } from "@/nodeDialog/composables/components/useFlowVariables";
import { getLongestCommonPrefix } from "@/nodeDialog/utils/paths";
import { computed } from "vue";
import FlowVariableControl from "./FlowVariableControl.vue";
import { useFlowVariableModel } from "@/nodeDialog/composables/components/useFlowVariableModel";
import SpecialFlowVariableControl from "./SpecialFlowVariableControl.vue";

const { configPaths } = getFlowVariableSettingsProvidedByControl();

const emit = defineEmits(["controllingFlowVariableSet"]);

const prefixLength = computed(() => {
  return getLongestCommonPrefix(
    configPaths.value.map((configPath) => configPath.configPath),
  ).length;
});

const flowVariableModel = useFlowVariableModel();
</script>

<template>
  <SpecialFlowVariableControl
    v-if="flowVariableModel"
    :flow-variable-model="flowVariableModel"
    :config-paths="configPaths"
  />
  <div v-else class="flex-gap-20">
    <FlowVariableControl
      v-for="configPath in configPaths"
      :key="configPath.configPath"
      :config-name="
        configPaths.length > 1
          ? configPath.configPath.slice(prefixLength)
          : null
      "
      :config-path="configPath"
      @controlling-flow-variable-set="
        ($event) => emit('controllingFlowVariableSet', $event)
      "
    />
  </div>
</template>

<style lang="postcss" scoped>
.flex-gap-20 {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
