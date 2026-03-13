<script setup lang="ts">
withDefaults(
  defineProps<{
    columnName: string;
    inline?: boolean;
  }>(),
  {
    inline: false,
  },
);
</script>

<template>
  <span
    class="column-label"
    :class="{ 'column-label-inline': inline }"
    :title="columnName"
    >{{ columnName }}:</span
  >
</template>

<style scoped>
.column-label {
  font-weight: 700;

  &.column-label-inline {
    margin-right: var(--space-4);
  }

  &:not(.column-label-inline) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }
}
</style>
