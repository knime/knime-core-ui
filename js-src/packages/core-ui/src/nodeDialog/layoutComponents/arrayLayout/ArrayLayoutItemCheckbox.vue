<script setup lang="ts">
import type { VueControlProps } from "@knime/jsonforms";
import { KdsCheckbox } from "@knime/kds-components";

defineProps<VueControlProps<boolean>>();
</script>

<template>
  <KdsCheckbox
    :model-value="control.data"
    :title="control.label"
    class="checkbox"
    @update:model-value="(value: boolean) => changeValue(value)"
  />
</template>

<style lang="postcss" scoped>
.checkbox {
  /** These styles ensure that the checkbox fits into the array layout items header */
  transform: translate(0, -1px);
  margin-right: var(--kds-spacing-container-0-5x);
}
</style>
