<script setup lang="ts">
import { ref } from "vue";

import { KdsInfoToggleButton, KdsPopover } from "@knime/kds-components";

import type { DescriptionPopoverProps } from "./types/DescriptionPopoverProps";

withDefaults(defineProps<DescriptionPopoverProps>(), {
  hover: false,
  ignoredClickOutsideTarget: null,
});

const open = ref(false);
</script>

<template>
  <KdsPopover
    v-model="open"
    :ignored-click-outside-target="ignoredClickOutsideTarget"
  >
    <template #activator>
      <KdsInfoToggleButton v-model="open" :visible="hover" />
    </template>
    <div v-html="html" />
  </KdsPopover>
</template>
