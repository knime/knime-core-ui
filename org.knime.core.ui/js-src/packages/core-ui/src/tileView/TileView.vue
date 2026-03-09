<script setup lang="ts">
import "../common/main.css";
import { computed, inject, nextTick, onMounted, ref, watchEffect } from "vue";

import { TopControls } from "@knime/knime-ui-table";
import type { TableConfig } from "@knime/knime-ui-table";
import {
  CachingSelectionService,
  JsonDataService,
  ReportingService,
  SharedDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

const getKnimeService = (inject("getKnimeService") ??
  (() => null)) as () => UIExtensionService;

import EmptyDataState from "@/common/EmptyDataState.vue";
import ViewTitle from "@/common/ViewTitle.vue";

import Tile from "./Tile.vue";
import { useSelection } from "./composables/useSelection";
import { type TileViewSettings, useSettings } from "./composables/useSettings";
import { useTableData } from "./composables/useTableData";
import { useTileResize } from "./composables/useTileResize";

const knimeService = getKnimeService();
const jsonDataService = new JsonDataService(knimeService);
const selectionService = new CachingSelectionService(knimeService);
const reportingService = new ReportingService(knimeService);

const currentPage = ref(0);
const tableSize = ref(0);
const isReportingActive = reportingService.isReportingActive();

const { settings, updateSettings } = useSettings();

const { table, fetchTable } = useTableData(
  jsonDataService,
  settings,
  currentPage,
);

const { selection, transformSelection, onSelectionChange, updateSelection } =
  useSelection(selectionService, table, settings, currentPage, fetchTable);

const { tileWidth, isResizeActive, firstTileRef } = useTileResize();

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

const showTitle = computed(() => table.value.rowTitles !== null);

const showTiles = computed(
  () => table.value.displayedColumns.length > 0 || showTitle.value,
);

const gridColumns = computed(() =>
  Math.min(settings.value.tilesPerRow, settings.value.pageSize),
);

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${gridColumns.value}, 1fr)`,
}));

const onPageChange = async (pageDirection: 1 | -1) => {
  currentPage.value += pageDirection;
  await fetchTable({ clearImageDataCache: true });
  transformSelection();
};

const tableConfig = computed<TableConfig>(() => ({
  pageConfig: {
    currentSize: table.value.rowCount,
    tableSize: tableSize.value,
    pageSize: settings.value.pageSize,
    currentPage: currentPage.value + 1, // 1-based for the UI component, 0-based internally
    columnCount: 0,
    showTableSize: true,
    showPageControls: !isReportingActive,
    rowLabel: "Showing",
  },
  settingsItems:
    settings.value?.showOnlySelectedRowsConfigurable && !isReportingActive
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
const isDataRendered = ref(false);

const pendingImages = ref(new Set<string>());
const imagesLoaded = computed(() => pendingImages.value.size === 0);
if (reportingService.isReportingActive()) {
  watchEffect(() => {
    if (isDataLoaded.value && isDataRendered.value && imagesLoaded.value) {
      reportingService.setRenderCompleted();
    }
  });
}

onMounted(async () => {
  const initialData = await jsonDataService.initialData();
  settings.value = initialData.settings;
  table.value = initialData.table;
  settings.value.displayedColumns.selected =
    settings.value.displayedColumns.selected ??
    initialData.table.displayedColumns;

  tableSize.value = initialData.table.rowCount;

  const sharedDataService = new SharedDataService(knimeService);
  sharedDataService.addSharedDataListener(onViewSettingsChange);

  await selectionService.initialSelection();
  transformSelection();
  selectionService.addOnSelectionChangeCallback(onSelectionChange);
  isDataLoaded.value = true;
  await nextTick();
  isDataRendered.value = true;
});
</script>

<template>
  <div class="tile-view-wrapper">
    <ViewTitle :title="settings.title" />
    <div v-if="isDataLoaded && showTiles" class="tile-view-wrapper-data">
      <TopControls
        class="top-controls"
        :table-config="tableConfig"
        @next-page="onPageChange(1)"
        @prev-page="onPageChange(-1)"
      />
      <div class="tile-grid-scroller">
        <div class="tile-grid" :style="gridStyle">
          <Tile
            v-for="(row, index) in table.rows"
            :ref="
              (comp: any) => {
                if (index === 0) firstTileRef = comp;
              }
            "
            :key="`row-${row[1]}-${index}`"
            :row-index="index"
            :row
            :title="table?.rowTitles?.[index] ?? null"
            :show-title
            :columns="table.displayedColumns"
            :text-alignment="settings.textAlignment"
            :display-column-headers="settings.displayColumnHeaders"
            :color="table?.rowColors?.[index] ?? null"
            :selection-mode="settings.selectionMode"
            :selected="selection[index]"
            :column-content-types="table.columnContentTypes"
            :tile-width="tileWidth"
            :is-resize-active="isResizeActive.state"
            :is-report="isReportingActive"
            @update-selection="updateSelection"
            @pending-image="(id: string) => pendingImages.add(id)"
            @rendered-image="(id: string) => pendingImages.delete(id)"
          />
        </div>
      </div>
    </div>
    <EmptyDataState v-else :is-data-loaded loading-animation-enabled />
  </div>
</template>

<style scoped>
.tile-view-wrapper {
  font-family: Roboto, sans-serif;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  min-width: 0;
  width: 100%;

  & .tile-view-wrapper-data {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
    min-width: 0;
    overflow: hidden;

    & .tile-grid-scroller {
      flex: 1;
      min-height: 0;
      overflow: hidden auto;

      & .tile-grid {
        display: grid;
        gap: 16px;
        flex: 0 0 auto;
        padding-bottom: 4px;
        padding-right: 4px;
      }
    }
  }
}
</style>
