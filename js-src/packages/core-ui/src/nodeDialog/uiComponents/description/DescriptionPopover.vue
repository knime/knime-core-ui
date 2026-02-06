<script setup lang="ts">
import { KdsInfoToggleButton } from "@knime/kds-components";

import type { DescriptionPopoverProps } from "./types/DescriptionPopoverProps";

withDefaults(defineProps<DescriptionPopoverProps>(), {
  hover: false,
  ignoredClickOutsideTarget: null,
});
</script>

<template>
  <KdsInfoToggleButton :hidden="!hover">
    <div v-html="html" />
  </KdsInfoToggleButton>
</template>
