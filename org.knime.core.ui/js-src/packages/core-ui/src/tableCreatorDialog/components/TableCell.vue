<script setup lang="ts">
/**
 * Same white space as the space between rows should also be to the next cell horizontally.
 */
const MARGIN_LEFT = 1;
/**
 * The left padding to apply inside the cell (matches the default table cell padding).
 */
const CELL_PADDING_LEFT = 10 - MARGIN_LEFT; // eslint-disable-line no-magic-numbers

defineProps<{
  value: string;
  isInvalid?: boolean;
  paddingTopBottom?: number;
}>();
</script>

<template>
  <div
    class="table-cell"
    :class="{ invalid: isInvalid }"
    :style="{
      paddingLeft: `${CELL_PADDING_LEFT}px`,
      marginLeft: `${MARGIN_LEFT}px`,
      ...(paddingTopBottom !== undefined
        ? {
            paddingTop: `${paddingTopBottom}px`,
            paddingBottom: `${paddingTopBottom}px`,
          }
        : {}),
    }"
  >
    {{ value }}
  </div>
</template>

<style scoped lang="postcss">
.table-cell {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  pointer-events: none;

  &.invalid {
    background-color: var(
      --kds-color-background-static-danger-muted,
      hsl(0deg 100% 97.1%)
    );
  }
}
</style>
