<script setup lang="ts">
import { Checkbox } from "@knime/components";
import type { VueControlProps } from "@knime/jsonforms";

defineProps<VueControlProps<boolean>>();
</script>

<template>
  <Checkbox
    :model-value="control.data"
    :title="control.label"
    class="checkbox"
    @update:model-value="changeValue"
  />
</template>

<style lang="postcss" scoped>
.checkbox {
  /** These styles ensure that the checkbox fits into the array layout items header */
  transform: translate(0, 5px);
  margin-right: -15px;
}
</style>
