<script setup lang="ts">
import "../common/main.css";
import { computed, nextTick, onMounted, ref, watchEffect } from "vue";

import { type TableConfig } from "@knime/knime-ui-table";
import { JsonDataService } from "@knime/ui-extension-service";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import getKnimeService from "@/tableView/utils/getKnimeService";

import TileViewDisplay from "./TileViewDisplay.vue";
import {
  DEFAULT_SETTINGS,
  type TileViewSettings,
} from "./composables/useSettings";
import { type TileViewTableData } from "./composables/useTableData";

const knimeService = getKnimeService();
const jsonDataService = new JsonDataService(knimeService);

const settings = ref<TileViewSettings>(DEFAULT_SETTINGS);
const table = ref<TileViewTableData>({
  columnContentTypes: [],
  rowCount: 0,
  rows: [],
  rowTitles: [],
  rowColors: [],
  displayedColumns: [],
});

const isDataLoaded = ref(false);
const isDataRendered = ref(false);
const pendingImages = ref(new Set<string>());
const imagesLoaded = computed(() => pendingImages.value.size === 0);

const emit = defineEmits<{
  rendered: [];
}>();

watchEffect(() => {
  if (isDataLoaded.value && isDataRendered.value && imagesLoaded.value) {
    emit("rendered");
  }
});

onMounted(async () => {
  const initialData = await jsonDataService.initialData();
  settings.value = {
    ...initialData.settings,
    selectionMode: SelectionMode.OFF,
  };
  settings.value.displayedColumns.selected =
    settings.value.displayedColumns.selected ??
    initialData.table.displayedColumns;

  table.value = (await jsonDataService.data({
    method: "getTable",
    options: [
      settings.value.displayedColumns.selected,
      settings.value.titleColumn,
      settings.value.colorColumn,
      0,
      initialData.table.rowCount,
      true,
      settings.value.showOnlySelectedRows,
    ],
  })) as TileViewTableData;

  isDataLoaded.value = true;
  await nextTick();
  isDataRendered.value = true;
});

const tableConfig = computed<TableConfig>(() => ({
  pageConfig: {
    currentSize: table.value.rows.length,
    currentPage: 1,
    tableSize: table.value.rowCount,
    pageSize: table.value.rows.length,
    columnCount: 0,
    showTableSize: true,
    showPageControls: false,
    rowLabel: "Showing",
  },
}));
</script>

<template>
  <TileViewDisplay
    :settings="settings"
    :table="table"
    :is-data-loaded="isDataLoaded"
    :selection="[]"
    :is-report="true"
    :table-config="tableConfig"
    @pending-image="(id: string) => pendingImages.add(id)"
    @rendered-image="(id: string) => pendingImages.delete(id)"
  />
</template>
