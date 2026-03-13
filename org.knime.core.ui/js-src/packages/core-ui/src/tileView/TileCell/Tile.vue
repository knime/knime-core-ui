<script setup lang="ts">
import type { Cell, ColumnContentType } from "@/tableView/types/Table";
import { SelectionMode } from "@/tableView/types/ViewSettings";
import { useTile } from "../composables/useTile";

import MissingValue from "./MissingValueCell.vue";
import TileCell from "./TileCell.vue";

const props = defineProps<{
  rowIndex: number;
  selected: boolean;
  row: (Cell | string | null)[];
  title: Cell | string | null;
  showTitle: boolean;
  columns: string[];
  displayColumnHeaders: boolean;
  color: string | null;
  selectionMode: SelectionMode;
  columnContentTypes: ColumnContentType[];
  tileWidth: number;
  isResizeActive: boolean;
  isReport: boolean;
}>();

const emit = defineEmits<{
  updateSelection: [string, boolean];
  pendingImage: [string];
  renderedImage: [string];
}>();

const { transformedRow, titleCell, showSelection, enableSelection, rowSpan } =
  useTile(props);
</script>

<template>
  <div
    class="tile"
    :class="{
      'selection-enabled': enableSelection,
      selected: showSelection && selected,
      colored: color !== null,
    }"
    :style="{
      gridRow: `span ${rowSpan}`,
      '--tile-color': color || undefined,
    }"
    @click="
      enableSelection &&
        emit('updateSelection', transformedRow[1].value, !selected)
    "
  >
    <div v-if="showTitle" class="tile-title" :title="titleCell.value">
      <MissingValue v-if="titleCell.isMissing" />
      <span v-else class="tile-title-text">{{ titleCell.value }}</span>
    </div>
    <TileCell
      v-for="(cell, index) in transformedRow.slice(2)"
      :key="`column-${row[1]}-${index}`"
      :cell="cell"
      :column-name="columns[index]"
      :display-column-headers
      :content-type="columnContentTypes[index]"
      :tile-width
      :is-resize-active
      :is-report
      @pending-image="(id: string) => emit('pendingImage', id)"
      @rendered-image="(id: string) => emit('renderedImage', id)"
    />
  </div>
</template>

<style scoped>
.tile {
  display: grid;
  grid-template-rows: subgrid;
  grid-template-columns: minmax(0, 1fr);
  padding: 12px;
  align-items: start;
  background: var(--knime-gray-ultra-light);
  border-radius: 8px;
  border: 1px solid var(--knime-porcelain);
  overflow: hidden;
  position: relative;
  min-width: 0;

  &.selection-enabled {
    cursor: pointer;

    &:hover {
      background: var(--knime-porcelain);
    }
  }

  &.colored {
    padding-top: 20px;

    &::before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 8px;
      background: var(--tile-color);
    }
  }

  &.selected {
    /* We use the hex code here, because the corresponding color variable of kds is not available in views. */
    background: #edf4ff; /* stylelint-disable-line color-no-hex */
  }

  & .tile-title {
    display: flex;
    flex-direction: column;
    font-weight: 700;
    font-size: 16px;
    color: var(--knime-black);
    line-height: 16px;
    min-width: 0;

    & .tile-title-text {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }
  }
}

@media print {
  .tile {
    break-inside: avoid;
    break-inside: avoid;
    -webkit-column-break-inside: avoid;
  }
}
</style>
