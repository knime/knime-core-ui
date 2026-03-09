<script setup lang="ts">
import { computed } from "vue";

import type { DataType } from "../types";

import SubHeaderTypeRendererBase from "./SubHeaderTypeRendererBase.vue";

const props = defineProps<{
  dataTypes: Record<string, DataType>;
  subHeader: string;
}>();

const dataType = computed(() => props.dataTypes[props.subHeader]);

const dataTypeId = computed(() => dataType.value?.id ?? "unknown-datatype");
const dataTypeName = computed(() => dataType.value?.name ?? "Unknown datatype");
</script>

<template>
  <SubHeaderTypeRendererBase
    :icon-name="dataTypeId"
    :data-type-name="dataTypeName"
  />
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
