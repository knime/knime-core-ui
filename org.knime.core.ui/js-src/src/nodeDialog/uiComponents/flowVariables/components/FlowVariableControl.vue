<script setup lang="ts">
import { ConfigPath } from "@/nodeDialog/composables/components/useFlowVariables";
import { Label } from "@knime/components";
import FlowVariableSelector from "./FlowVariableSelector.vue";
import FlowVariableExposer from "./FlowVariableExposer.vue";

defineProps<{
  configName: string | null;
  configPath: ConfigPath;
  onlyControlling?: boolean;
}>();

const emit = defineEmits(["controllingFlowVariableSet"]);
</script>

<template>
  <div class="flex-gap-10">
    <Label
      #default="{ labelForId }"
      :text="
        configName === null
          ? 'Overwrite with flow variable'
          : 'Overwrite '.concat(configName)
      "
      class="label"
    >
      <FlowVariableSelector
        :id="labelForId"
        :data-path="configPath.dataPath"
        :persist-path="configPath.configPath"
        @controlling-flow-variable-set="
          emit('controllingFlowVariableSet', $event)
        "
      />
    </Label>
    <Label
      v-if="!onlyControlling"
      #default="{ labelForId }"
      :text="
        configName === null
          ? 'Output as flow variable'
          : 'Output '.concat(configName)
      "
      class="label"
    >
      <FlowVariableExposer
        :id="labelForId"
        :persist-path="configPath.configPath"
      />
    </Label>
  </div>
</template>

<style lang="postcss" scoped>
.flex-gap-10 {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
