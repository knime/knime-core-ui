<script setup lang="ts">
import { type VueControlProps } from "@knime/jsonforms";

import FieldControl from "../FieldControl.vue";

import type { DBTableSelection } from "./types";

defineProps<VueControlProps<DBTableSelection>>();
</script>

<template>
  <div class="flex-column">
    <!-- TODO(UIEXT-2744) implement basic browser -->
    <FieldControl field-name="schemaName" :control />
    <FieldControl field-name="tableName" :control />
  </div>
</template>

<style lang="postcss" scoped>
.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}
</style>
