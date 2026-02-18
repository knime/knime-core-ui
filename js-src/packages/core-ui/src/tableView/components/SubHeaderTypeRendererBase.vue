<script setup lang="ts">
import { KdsDataType } from "@knime/kds-components";

defineProps<{
  iconName: string;
  dataTypeName: string;
}>();
</script>

<template>
  <div class="data-type-sub-header">
    <KdsDataType :icon-name :icon-title="dataTypeName" size="small" />
    <span>{{ dataTypeName }}</span>
  </div>
</template>

<style lang="postcss" scoped>
.data-type-sub-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);

  & > :first-child {
    flex: 0 0 auto;
  }

  & > span {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
