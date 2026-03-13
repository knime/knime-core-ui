<script setup lang="ts">
import { computed } from "vue";

import MultiLineTextRenderer from "@/tableView/renderers/MultiLineTextRenderer.vue";
import type { ColumnContentType } from "@/tableView/types/Table";

import ColumnLabel from "./ColumnLabel.vue";
import MissingValueCell from "./MissingValueCell.vue";

const props = defineProps<{
  cell: { value: string; isMissing: boolean };
  columnName: string;
  displayColumnHeaders: boolean;
  contentType: ColumnContentType;
}>();

const isMultiLineText = computed(() => props.contentType === "multi_line_txt");
const hasInlineValueLabel = computed(
  () =>
    props.displayColumnHeaders &&
    (props.cell.isMissing || isMultiLineText.value),
);
</script>

<template>
  <div class="tile-row">
    <ColumnLabel
      v-if="displayColumnHeaders && !hasInlineValueLabel"
      :column-name="columnName"
    />
    <div
      class="tile-value"
      :class="{ 'tile-value-inline-label': hasInlineValueLabel }"
      :title="cell.value"
    >
      <div v-if="hasInlineValueLabel" class="inline-labeled-text">
        <ColumnLabel :column-name="columnName" :inline="true" />
        <MissingValueCell v-if="cell.isMissing" />
        <MultiLineTextRenderer
          v-else
          class="inline-multiline-content"
          :text="cell.value"
        />
      </div>
      <div v-else class="cell-value">{{ cell.value }}</div>
    </div>
  </div>
</template>

<style scoped>
.tile-row {
  color: var(--knime-black);
  font-size: 13px;
  line-height: 19.5px;
  display: flex;
  gap: 4px;
  align-items: flex-start;
  min-width: 0;

  & .tile-value {
    flex: 1;
    min-width: 0;
    overflow: hidden;

    &.tile-value-inline-label {
      overflow: visible;
    }

    & .inline-multiline-content {
      display: inline;
      min-width: 0;
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }

  & .inline-labeled-text {
    display: block;
  }

  & :deep(.column-label) {
    flex-shrink: 0;
  }

  & .cell-value {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
