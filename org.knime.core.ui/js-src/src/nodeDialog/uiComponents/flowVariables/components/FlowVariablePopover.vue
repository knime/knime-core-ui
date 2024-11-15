<script setup lang="ts">
import DeprecatedFlowVariables from "./DeprecatedFlowVariables.vue";
import useDeprecatedConfigPaths from "../composables/useDeprecatedConfigPaths";
import FlowVariablePopoverContent from "./ModernFlowVariablePopover.vue";

const { deprecatedSetConfigPaths } = useDeprecatedConfigPaths();

const emit = defineEmits(["controllingFlowVariableSet"]);
</script>

<template>
  <DeprecatedFlowVariables v-if="deprecatedSetConfigPaths.length" />
  <FlowVariablePopoverContent
    v-else
    @controlling-flow-variable-set="
      ($event) => emit('controllingFlowVariableSet', $event)
    "
  />
</template>
