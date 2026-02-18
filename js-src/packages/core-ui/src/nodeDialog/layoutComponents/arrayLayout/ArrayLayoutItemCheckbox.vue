<script setup lang="ts">
import type { VueControlProps } from "@knime/jsonforms";
import { KdsCheckbox, type KdsCheckboxProps } from "@knime/kds-components";

type CheckboxValue = KdsCheckboxProps["modelValue"];

defineProps<VueControlProps<boolean>>();
</script>

<template>
  <KdsCheckbox
    :model-value="control.data"
    :title="control.label"
    class="checkbox"
    @update:model-value="
      (value: CheckboxValue) => changeValue(value as boolean)
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
