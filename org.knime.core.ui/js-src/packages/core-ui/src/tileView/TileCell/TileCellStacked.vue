<script setup lang="ts">
import { computed } from "vue";

import HtmlRenderer from "@/tableView/renderers/HtmlRenderer.vue";
import ImageRenderer from "@/tableView/renderers/ImageRenderer.vue";
import type { ColumnContentType } from "@/tableView/types/Table";

import ColumnLabel from "./ColumnLabel.vue";
import MissingValueCell from "./MissingValueCell.vue";

const props = defineProps<{
  cell: { value: string; isMissing: boolean };
  columnName: string;
  displayColumnHeaders: boolean;
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
</script>

<template>
  <div class="tile-row">
    <ColumnLabel v-if="displayColumnHeaders" :column-name="columnName" />
    <div class="tile-value" :title="isImage ? undefined : cell.value">
      <MissingValueCell v-if="cell.isMissing" />
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
        class="stacked-html-content"
        :content="cell.value"
        :used-for-auto-size-calculation="undefined"
      />
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

  & .tile-value {
    min-width: 0;
    overflow: hidden;

    & .stacked-html-content {
      display: block;
      min-width: 0;
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }
}
</style>
