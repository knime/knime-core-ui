<script setup lang="ts">
import type { pad } from 'lodash-es';

/**
 * Same white space as the space between rows should also be to the next cell horizontally.
 */
const MARGIN_LEFT = 1;
/**
 * The left padding to apply inside the cell (matches the default table cell padding).
 */
const CELL_PADDING_LEFT = 10 - MARGIN_LEFT;


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
        ? { paddingTop: `${paddingTopBottom}px`, paddingBottom: `${paddingTopBottom}px` }
        : {}),
    }"
  >
    <span>
      {{ value }}
    </span>
  </div>
</template>

<style scoped lang="postcss">
.table-cell {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  /**
   * TODO: Check whether this is now also needed in the TableView!
   */
  pointer-events: none;

  &.invalid {
    /**
     * TODO: Change to --kds-color-background-static-danger-muted once available
     */
    background-color: var(--kds-color-background-danger-active);
  }
}
</style>
