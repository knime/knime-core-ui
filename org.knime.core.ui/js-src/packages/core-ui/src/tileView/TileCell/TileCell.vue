<script setup lang="ts">
import { computed } from "vue";

import type { ColumnContentType } from "@/tableView/types/Table";

import TileCellInline from "./TileCellInline.vue";
import TileCellStacked from "./TileCellStacked.vue";

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
const isStackedLayout = computed(() => isHTML.value || isImage.value);
</script>

<template>
  <TileCellStacked
    v-if="isStackedLayout"
    :cell
    :column-name
    :display-column-headers
    :content-type
    :tile-width
    :is-resize-active
    :is-report
    @pending-image="(id: string) => emit('pendingImage', id)"
    @rendered-image="(id: string) => emit('renderedImage', id)"
  />
  <TileCellInline
    v-else
    :cell
    :column-name
    :display-column-headers
    :content-type
  />
</template>
