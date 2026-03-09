<script setup lang="ts">
import { Checkbox } from "@knime/components";

import type { Cell, ColumnContentType } from "@/tableView/types/Table";
import { SelectionMode } from "@/tableView/types/ViewSettings";

import MissingValue from "./MissingValueCell.vue";
import TileCell from "./TileCell.vue";
import { useTile } from "./composables/useTile";

const props = defineProps<{
  rowIndex: number;
  selected: boolean;
  row: (Cell | string | null)[];
  title: Cell | string | null;
  showTitle: boolean;
  columns: string[];
  textAlignment: "LEFT" | "CENTER" | "RIGHT";
  displayColumnHeaders: boolean;
  color: string | null;
  selectionMode: SelectionMode;
  columnContentTypes: ColumnContentType[];
  tileWidth: number;
  isResizeActive: boolean;
  isReport: boolean;
}>();

const emit = defineEmits<{
  "update-selection": [string, boolean];
  pendingImage: [string];
  renderedImage: [string];
}>();

const {
  transformedRow,
  textAlign,
  iconAlign,
  titleCell,
  showSelection,
  enableSelection,
  rowSpan,
} = useTile(props);

const onUpdateSelection = (selected: boolean) => {
  emit("update-selection", transformedRow.value[1].value, selected);
};
</script>

<template>
  <div
    class="tile"
    :class="{
      selected: showSelection && props.selected,
      colored: props.color !== null,
    }"
    :style="{
      textAlign,
      gridRow: `span ${rowSpan}`,
      '--tile-color': props.color || undefined,
      cursor: enableSelection ? 'pointer' : 'default',
    }"
    @click="enableSelection && onUpdateSelection(!props.selected)"
  >
    <div v-if="props.showTitle" class="tile-title" :title="titleCell.value">
      <MissingValue
        v-if="titleCell.isMissing"
        :style="{ alignSelf: iconAlign }"
      />
      <span v-else class="tile-title-text">{{ titleCell.value }}</span>
    </div>
    <TileCell
      v-for="(cell, index) in transformedRow.slice(2)"
      :key="`column-${props.row[1]}-${index}`"
      :cell="cell"
      :column-name="props.columns[index]"
      :display-column-headers
      :icon-align
      :content-type="props.columnContentTypes[index]"
      :tile-width
      :is-resize-active
      :is-report
      @pending-image="(id: string) => emit('pendingImage', id)"
      @rendered-image="(id: string) => emit('renderedImage', id)"
    />
    <div v-if="showSelection" class="tile-selection">
      <Checkbox
        :id="`row-${props.rowIndex}`"
        :disabled="!enableSelection"
        :model-value="props.selected"
        @update:model-value="onUpdateSelection"
        >selected</Checkbox
      >
    </div>
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
    font-size: 14px;
    color: var(--knime-black);
    line-height: 14px;
    min-width: 0;

    & .tile-title-text {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }
  }

  & .tile-selection {
    height: 20px;
  }
}
</style>
