<script setup lang="ts">
import { ref } from "vue";

import { KdsInfoToggleButton, KdsPopover } from "@knime/kds-components";

import type { DescriptionPopoverProps } from "./types/DescriptionPopoverProps";

withDefaults(defineProps<DescriptionPopoverProps>(), {
  hover: false,
  ignoredClickOutsideTarget: null,
});

const open = ref(false);
const activatorEl = ref<HTMLButtonElement | null>(null);
</script>

<template>
  <KdsInfoToggleButton ref="activatorEl" v-model="open" :hidden="!hover" />
  <KdsPopover
    v-model="open"
    :activator-el="activatorEl"
    :ignored-click-outside-target="ignoredClickOutsideTarget"
    placement="top-left"
    style="width: max-content; max-width: 400px"
  >
    <div v-html="html" />
  </KdsPopover>
</template>
