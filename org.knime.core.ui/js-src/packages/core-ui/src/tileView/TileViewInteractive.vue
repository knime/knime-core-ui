<script setup lang="ts">
import "../common/main.css";
import { computed, onMounted, ref } from "vue";

import type { TableConfig } from "@knime/knime-ui-table";
import {
  CachingSelectionService,
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

import getKnimeService from "@/tableView/utils/getKnimeService";

import TileViewDisplay from "./TileViewDisplay.vue";
import { useSelection } from "./composables/useSelection";
import { type TileViewSettings, useSettings } from "./composables/useSettings";
import { useTableData } from "./composables/useTableData";

const knimeService = getKnimeService();
const jsonDataService = new JsonDataService(knimeService);
const selectionService = new CachingSelectionService(knimeService);

const currentPage = ref(0);
const underlyingTableSize = ref(0);

const { settings, updateSettings } = useSettings();

const { table, fetchTable } = useTableData(
  jsonDataService,
  settings,
  currentPage,
);

const { selection, transformSelection, onSelectionChange, updateSelection } =
  useSelection(selectionService, table, settings, currentPage, fetchTable);

const handleNewSettings = async (newSettings: TileViewSettings) => {
  const diff = updateSettings(settings.value, newSettings);
  if (diff.needsRefetch) {
    if (diff.needsPageReset) {
      currentPage.value = 0;
    }
    await fetchTable({
      displayedColumns: diff.displayedColumnsChanged
        ? newSettings.displayedColumns.selected
        : table.value.displayedColumns,
      clearImageDataCache: diff.needsPageReset,
    });
    transformSelection();
  }
};

const onViewSettingsChange = (payload: unknown) => {
  const viewSettings = (payload as { data: { view: TileViewSettings } }).data
    .view;
  handleNewSettings({
    ...viewSettings,
    displayedColumns: {
      selected:
        viewSettings.displayedColumns.selected ??
        settings.value.displayedColumns.selected,
    },
  });
};

const onPageChange = async (pageDirection: 1 | -1) => {
  currentPage.value += pageDirection;
  await fetchTable({ clearImageDataCache: true });
  transformSelection();
};

const tableConfig = computed<TableConfig>(() => ({
  pageConfig: {
    currentSize: table.value.rowCount,
    tableSize: underlyingTableSize.value,
    pageSize: settings.value.pageSize,
    currentPage: currentPage.value + 1, // 1-based for the UI component, 0-based internally
    columnCount: 0,
    showTableSize: true,
    showPageControls: true,
    rowLabel: "Showing",
  },
  settingsItems: settings.value?.showOnlySelectedRowsConfigurable
    ? [
        {
          text: "Show only selected rows",
          checkbox: {
            checked: settings.value.showOnlySelectedRows,
            setBoolean: async (checked: boolean) => {
              settings.value.showOnlySelectedRows = checked;
              currentPage.value = 0;
              await fetchTable({ clearImageDataCache: true });
              transformSelection();
            },
          },
        },
      ]
    : [],
}));

const isDataLoaded = ref(false);

onMounted(async () => {
  const initialData = await jsonDataService.initialData();
  settings.value = initialData.settings;
  table.value = initialData.table;
  settings.value.displayedColumns.selected =
    settings.value.displayedColumns.selected ??
    initialData.table.displayedColumns;

  underlyingTableSize.value = initialData.table.rowCount;

  const sharedDataService = new SharedDataService(knimeService);
  sharedDataService.addSharedDataListener(onViewSettingsChange);

  await selectionService.initialSelection();
  transformSelection();
  selectionService.addOnSelectionChangeCallback(onSelectionChange);
  isDataLoaded.value = true;
});
</script>

<template>
  <TileViewDisplay
    :settings="settings"
    :table="table"
    :is-data-loaded="isDataLoaded"
    :selection="selection"
    :is-report="false"
    :table-config="tableConfig"
    @update-selection="updateSelection"
    @next-page="onPageChange(1)"
    @prev-page="onPageChange(-1)"
  />
</template>
