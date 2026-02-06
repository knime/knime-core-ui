<script setup lang="ts">
import type { VueControlProps } from "@knime/jsonforms";
import { KdsCheckbox, type KdsCheckboxValue } from "@knime/kds-components";

defineProps<VueControlProps<boolean>>();
</script>

<template>
  <KdsCheckbox
    :model-value="control.data"
    :title="control.label"
    class="checkbox"
    @update:model-value="
      (value: KdsCheckboxValue) => changeValue(value as boolean)
    "
  />
</template>

<style lang="postcss" scoped>
.checkbox {
  /** These styles ensure that the checkbox fits into the array layout items header */
  transform: translate(0, -2px);
  margin-right: var(--kds-spacing-container-0-5x) !important;
}
</style>
