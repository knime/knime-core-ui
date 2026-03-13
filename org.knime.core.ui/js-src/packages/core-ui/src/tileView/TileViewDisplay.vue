<script setup lang="ts">
import { type ComponentPublicInstance, computed } from "vue";

import { TopControls } from "@knime/knime-ui-table";
import type { TableConfig } from "@knime/knime-ui-table";

import EmptyDataState from "@/common/EmptyDataState.vue";
import ViewTitle from "@/common/ViewTitle.vue";

import Tile from "./TileCell/Tile.vue";
import type { TileViewSettings } from "./composables/useSettings";
import type { TileViewTableData } from "./composables/useTableData";
import { useTileResize } from "./composables/useTileResize";

type TileInstance = InstanceType<typeof Tile>;

const props = defineProps<{
  settings: TileViewSettings;
  table: TileViewTableData;
  isDataLoaded: boolean;
  selection: boolean[];
  isReport: boolean;
  tableConfig?: TableConfig;
}>();

const emit = defineEmits<{
  updateSelection: [string, boolean];
  pendingImage: [string];
  renderedImage: [string];
  nextPage: [];
  prevPage: [];
}>();

const { tileWidth, isResizeActive, firstTileRef } = useTileResize();

const showTitle = computed(() => props.table.rowTitles !== null);

const showTiles = computed(
  () => props.table.displayedColumns.length > 0 || showTitle.value,
);

const gridColumns = computed(() =>
  props.isReport
    ? props.settings.tilesPerRow
    : Math.min(props.settings.tilesPerRow, props.settings.pageSize),
);

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${gridColumns.value}, 1fr)`,
}));
</script>

<template>
  <div class="tile-view-wrapper">
    <ViewTitle :title="settings.title" />
    <div v-if="isDataLoaded && showTiles" class="tile-view-wrapper-data">
      <TopControls
        :table-config="tableConfig"
        @next-page="emit('nextPage')"
        @prev-page="emit('prevPage')"
      />
      <div class="tile-grid-scroller">
        <div class="tile-grid" :style="gridStyle">
          <Tile
            v-for="(row, index) in table.rows"
            :ref="
              (ref: Element | ComponentPublicInstance | null) => {
                if (index === 0) firstTileRef = ref as TileInstance | null;
              }
            "
            :key="`row-${row[1]}-${index}`"
            :row-index="index"
            :row
            :title="table?.rowTitles?.[index] ?? null"
            :show-title
            :columns="table.displayedColumns"
            :display-column-headers="settings.displayColumnHeaders"
            :color="table?.rowColors?.[index] ?? null"
            :selection-mode="settings.selectionMode"
            :selected="selection[index] ?? false"
            :column-content-types="table.columnContentTypes"
            :tile-width
            :is-resize-active="isResizeActive.state"
            :is-report
            @update-selection="
              (rowId: string, selected: boolean) =>
                emit('updateSelection', rowId, selected)
            "
            @pending-image="(id: string) => emit('pendingImage', id)"
            @rendered-image="(id: string) => emit('renderedImage', id)"
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

@media print {
  .tile-view-wrapper {
    height: auto;

    & .tile-view-wrapper-data {
      flex: 0 0 auto;

      & .tile-grid-scroller {
        flex: 0 0 auto;

        & .tile-grid {
          padding-right: 0;
        }
      }
    }
  }
}
</style>
