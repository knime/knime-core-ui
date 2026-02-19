<script setup lang="ts">
import { computed } from "vue";

import { Checkbox } from "@knime/components";

export interface TileCell {
  column: string;
  value: string;
}

const props = defineProps<{
  rowIndex: number;
  cells: TileCell[];
  title: string;
  selected: boolean;
  textAlignment: "LEFT" | "CENTER" | "RIGHT";
  displayColumnHeaders: boolean;
}>();

defineEmits<{
  "toggle-selection": [];
}>();

const textAlign = computed(
  () => props.textAlignment.toLowerCase() as "left" | "center" | "right",
);
</script>

<template>
  <div class="tile" :class="{ selected }">
    <div v-if="title" class="tile-title">{{ title }}</div>
    <div
      v-for="cell in cells"
      :key="cell.column"
      class="tile-row"
      :style="{ textAlign }"
    >
      <p v-if="displayColumnHeaders" class="column-label">{{ cell.column }}:</p>
      <p class="cell-value">{{ cell.value }}</p>
    </div>
    <div class="tile-footer">
      <Checkbox
        :id="`row-${rowIndex}`"
        :model-value="selected"
        @update:model-value="$emit('toggle-selection')"
        >selected</Checkbox
      >
    </div>
  </div>
</template>

<style scoped>
.tile {
  /* background: var(--knime-white); */
  border-top: 4px solid var(--knime-yellow);
  border-radius: 4px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 1px 4px rgb(0 0 0 / 12%);
  box-sizing: border-box;

  &.selected {
    outline: 2px solid var(--knime-yellow);
    outline-offset: -2px;
  }
}

.tile-title {
  font-family: Roboto, sans-serif;
  font-weight: 700;
  font-size: 14px;
  color: var(--knime-masala);
  margin-bottom: 8px;
}

.tile-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 4px;
}

.column-label {
  font-family: Roboto, sans-serif;
  font-weight: 700;
  font-size: 13px;
  color: var(--knime-masala);
  margin: 0;
}

.cell-value {
  font-family: Roboto, sans-serif;
  font-weight: 400;
  font-size: 13px;
  color: var(--knime-masala);
  margin: 0;
  white-space: pre-wrap;

  /* word-break: break-word; */
}
</style>
