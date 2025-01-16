<script setup lang="ts">
import {
  type UseFlowSettingsProps,
  useFlowSettings,
} from "../useFlowVariables";

const props = defineProps<UseFlowSettingsProps>();

defineExpose(useFlowSettings(props));
</script>

<template>
  <slot />
</template>
