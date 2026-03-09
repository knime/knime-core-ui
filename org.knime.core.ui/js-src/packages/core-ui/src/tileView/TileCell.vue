<script setup lang="ts">
import { computed } from "vue";

import HtmlRenderer from "@/tableView/renderers/HtmlRenderer.vue";
import ImageRenderer from "@/tableView/renderers/ImageRenderer.vue";
import MultiLineTextRenderer from "@/tableView/renderers/MultiLineTextRenderer.vue";
import type { ColumnContentType } from "@/tableView/types/Table";

import MissingValueCell from "./MissingValueCell.vue";

const props = defineProps<{
  cell: { value: string; isMissing: boolean };
  columnName: string;
  displayColumnHeaders: boolean;
  iconAlign: string;
  contentType: ColumnContentType;
  tileWidth: number;
  isResizeActive: boolean;
  isReport: boolean;
}>();

const emit = defineEmits<{
  pendingImage: [string];
  renderedImage: [string];
}>();

const isHTML = computed(() => props.contentType === "html");
const isImage = computed(() => props.contentType === "img_path");
const isMultiLineText = computed(() => props.contentType === "multi_line_txt");
</script>

<template>
  <div class="tile-row">
    <span v-if="displayColumnHeaders" class="column-label" :title="columnName"
      >{{ columnName }}:</span
    >
    <div :title="isImage ? undefined : cell.value">
      <MissingValueCell
        v-if="cell.isMissing"
        :style="{ alignSelf: iconAlign }"
      />
      <ImageRenderer
        v-else-if="isImage"
        :include-data-in-html="isReport"
        :path="cell.value"
        :width="tileWidth"
        :update="isResizeActive"
        :table-is-ready="true"
        @pending="(id: string) => emit('pendingImage', id)"
        @rendered="(id: string) => emit('renderedImage', id)"
      />
      <HtmlRenderer
        v-else-if="isHTML"
        :content="cell.value"
        :used-for-auto-size-calculation="undefined"
      />
      <MultiLineTextRenderer v-else-if="isMultiLineText" :text="cell.value" />
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
  flex-direction: column;
  min-width: 0;

  & .column-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
    font-weight: 700;
  }

  & .cell-value {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
