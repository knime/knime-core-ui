<script setup lang="ts">
import { type Ref, computed, onMounted, reactive, ref, toRefs } from "vue";

import {
  type ColumnConfig,
  type Rect,
  TableUIWithAutoSizeCalculation,
} from "@knime/knime-ui-table";
import type { DataValueViewConfig } from "@knime/ui-extension-service/internal";

import EmptyDataState from "@/common/EmptyDataState.vue";
import ViewTitle from "@/common/ViewTitle.vue";

import SubHeaderTypeRenderer from "./components/SubHeaderTypeRenderer.vue";
import useAutoSizes from "./composables/useAutoSizes";
import useColumnSizes from "./composables/useColumnSizes";
import { BORDER_BOTTOM_WIDTH } from "./constants";
import CodeRenderer from "./renderers/CodeRenderer.vue";
import HtmlRenderer from "./renderers/HtmlRenderer.vue";
import ImageRenderer from "./renderers/ImageRenderer.vue";
import MultiLineTextRenderer from "./renderers/MultiLineTextRenderer.vue";
import type { HeaderMenuItem, TableViewDisplayProps } from "./types";
import { RowHeightMode } from "./types/ViewSettings";
import getDataConfig from "./utils/getDataConfig";
import getTableConfig from "./utils/getTableConfig";
import { separateSpecialColumns } from "./utils/specialColumns";
import useBoolean from "./utils/useBoolean";

const emit = defineEmits<{
  pageChange: [pageNumberDiff: -1 | 1];
  columnSort: [colInd: number, columnId: string | symbol];
  dataValueView: [
    row: { indexInInput: number; isTop: boolean },
    colInd: number,
    rect: DataValueViewConfig["anchor"],
  ];
  closeDataValueView: [];
  rowSelect: [
    selected: boolean,
    rowInd: number,
    groupInd: number,
    isTop: boolean,
  ];
  selectAll: [selected: boolean];
  search: [input: string];
  columnFilter: [colId: string | symbol, value: string | string[]];
  clearFilter: [];
  columnResize: [columnIndex: number, newColumnSize: number];
  headerSubMenuItemSelection: [item: HeaderMenuItem, colId: string];
  lazyload: [
    lazyloadParams: {
      direction: 1 | -1;
      startIndex: number;
      endIndex: number;
    },
  ];
  allColumnsResize: [newColumnSize: number];
  "update:available-width": [newAvailableWidth: number];
  copySelection: [
    copySelectionParams: {
      columnNames: string[];
      withRowIndices: boolean;
      withRowKeys: boolean;
      withHeaders: boolean;
      fromIndex: number;
      toIndex: number;
      isTop: boolean;
    },
  ];

  updateColumnConfigs: [newColumnConfigs: ColumnConfig[]];
  pendingImage: [imageId: string];
  renderedImage: [imageId: string];
  rowHeightUpdate: [newRowHeight: number | null | "dynamic"];
  tableIsReady: [];
}>();

const props = defineProps<TableViewDisplayProps>();

const root: Ref<null | HTMLElement> = ref(null);

const baseUrl = ref("");

const loadingAnimationEnabled = ref(false);
const TIMEOUT_HIDE_LOADING_ANIMATION = 300;

onMounted(() => {
  // @ts-ignore
  baseUrl.value = props.knimeService?.extensionConfig?.resourceInfo?.baseUrl;
  setTimeout(() => {
    loadingAnimationEnabled.value = true;
  }, TIMEOUT_HIDE_LOADING_ANIMATION);
});

const numberOfDisplayedIdColumns = computed(() => {
  let offset = props.settings.showRowKeys ? 1 : 0;
  offset += props.settings.showRowIndices ? 1 : 0;
  return offset;
});
const numberOfDisplayedRemainingColumns = computed(() =>
  props.header.indicateRemainingColumnsSkipped ? 1 : 0,
);

const numberOfDisplayedColumns = computed(
  () =>
    props.header.displayedColumns.length +
    numberOfDisplayedIdColumns.value +
    numberOfDisplayedRemainingColumns.value,
);
// The columns sent to the TableUI. The rowIndex and rowKey are included but might not be displayed.
const numberOfUsedColumns = computed(
  () =>
    props.header.displayedColumns.length +
    2 +
    numberOfDisplayedRemainingColumns.value,
);

const {
  header,
  settings,
  firstRowImageDimensions,
  currentRowHeight,
  enableDynamicRowHeight,
} = toRefs(props);

const {
  autoColumnSizes,
  autoColumnSizesActive,
  autoColumnSizesOptions,
  autoRowHeightOptions,
  onAutoColumnSizesUpdate,
} = useAutoSizes({
  settings,
  firstRowImageDimensions,
  currentRowHeight,
  enableDynamicRowHeight,
});

const {
  columnSizes,
  onColumnResize,
  onAllColumnsResize,
  onUpdateAvailableWidth,
  deleteColumnSizeOverrides,
} = useColumnSizes({
  header,
  settings,
  autoColumnSizes,
  autoColumnSizesActive,
});

const hasDynamicRowHeight = computed(
  () =>
    enableDynamicRowHeight.value &&
    settings.value.rowHeightMode === RowHeightMode.AUTO,
);

const structuralDataConfig = computed(() =>
  getDataConfig({
    settings: props.settings,
    columnSizes: [],
    enableRowResizing: props.enableRowResizing,
    hasDynamicRowHeight: hasDynamicRowHeight.value,
    currentRowHeight,
    ...reactive(props.header),
  }),
);
const structuralColumnConfigs = computed(
  () => structuralDataConfig.value.columnConfigs,
);
const rowConfig = computed(() => structuralDataConfig.value.rowConfig);

const rowColumnKeys = computed(() =>
  structuralColumnConfigs.value.map((columnConfig) => columnConfig.key),
);
const rowColumnSizes = computed(() =>
  structuralColumnConfigs.value.map(
    (columnConfig, index) => columnSizes.value[index] ?? columnConfig.size,
  ),
);
const rowColumnFormatters = computed(() =>
  structuralColumnConfigs.value.map((columnConfig) => columnConfig.formatter),
);
const rowColumnClassGenerators = computed(() =>
  structuralColumnConfigs.value.map(
    (columnConfig) => columnConfig.classGenerator,
  ),
);
const rowColumnHasSlotContent = computed(() =>
  structuralColumnConfigs.value.map(
    (columnConfig) => columnConfig.hasSlotContent,
  ),
);
const rowColumnHasDataValueView = computed(() =>
  structuralColumnConfigs.value.map(
    (columnConfig) => columnConfig.hasDataValueView,
  ),
);
const rowColumnNoPadding = computed(() =>
  structuralColumnConfigs.value.map((columnConfig) => columnConfig.noPadding),
);
const rowColumnNoPaddingLeft = computed(() =>
  structuralColumnConfigs.value.map(
    (columnConfig) => columnConfig.noPaddingLeft,
  ),
);
const rowColumnClickables = computed(() =>
  structuralColumnConfigs.value.map((columnConfig) =>
    Boolean(columnConfig.popoverRenderer),
  ),
);

const dataConfig = computed(() => ({
  columnConfigs: structuralColumnConfigs.value,
  rowConfig: rowConfig.value,
}));

const columnIds = computed(() =>
  structuralColumnConfigs.value.map((columnConfig) => columnConfig.id),
);

const tableConfig = computed(() =>
  getTableConfig({
    settings: props.settings,
    pageParams: props.page,
    sortParams: props.sorting,
    globalSearchQuery: props.globalSearchQuery,
    enableVirtualScrolling: props.enableVirtualScrolling,
    enableCellSelection: props.enableCellSelection,
    enableColumnResizing: props.enableColumnResizing,
    enableDataValueViews: props.enableDataValueViews,
    dataValueViewIsShown: props.dataValueViewIsShown,
    forceHideTableSizes: props.forceHideTableSizes || false,
    settingsItems: props.settingsItems,
  }),
);

// data
const appendDotsIfColumnsSkipped = (rows: any[]) => {
  if (props.header.indicateRemainingColumnsSkipped) {
    return rows.map((row) => [...row, "…"]);
  } else {
    return rows;
  }
};

const rowData = computed(() => appendDotsIfColumnsSkipped(props.rows.top));
const bottomRowData = computed(() =>
  props.rows.bottom ? appendDotsIfColumnsSkipped(props.rows.bottom) : [],
);

// map index to columnId. Used to transform params of emitted events
const getColumnId = (colIndex: number) => columnIds.value[colIndex];

// for slots
const getContentType = (index: number) =>
  props.header.columnContentTypes[index - 2];
const columnResizeActive = useBoolean();

const table: Ref<null | typeof TableUIWithAutoSizeCalculation> = ref(null);

const tableIsReady = ref(false);
const onTableIsReady = () => {
  emit("tableIsReady");
  tableIsReady.value = true;
};

defineExpose({
  ...[
    "refreshScroller" as const,
    "clearCellSelection" as const,
    "triggerCalculationOfAutoColumnSizes" as const,
  ].reduce((acc: Record<string, Function>, methodName) => {
    acc[methodName] = () => {
      const method = table.value?.[methodName];
      if (typeof method === "function") {
        method();
      }
    };
    return acc;
  }, {}),
  deleteColumnSizeOverrides,
});

const onCopySelection = ({
  rect: { x, y },
  id,
  withHeaders,
}: {
  rect: Rect;
  id: boolean;
  withHeaders: boolean;
}) => {
  const indices = Array.from(
    { length: x.max - x.min + 1 },
    (_, index) => x.min + index,
  );
  const { columnNames, containedSpecialColumns } = separateSpecialColumns(
    indices.map(getColumnId),
  );
  const fromIndex = y.min;
  const toIndex = y.max;
  emit("copySelection", {
    columnNames,
    withRowIndices: containedSpecialColumns.has("INDEX"),
    withRowKeys: containedSpecialColumns.has("ROW_ID"),
    withHeaders,
    fromIndex,
    toIndex,
    isTop: id,
  });
};

const onDataValueView = (
  row: { indexInInput: number; isTop: boolean },
  colIndex: number,
  rect: DataValueViewConfig["anchor"],
) =>
  emit("dataValueView", row, colIndex - numberOfDisplayedIdColumns.value, rect);

const onCloseDataValueView = () => emit("closeDataValueView");

const useCodeRenderer = (index: number) =>
  ["xml", "json"].includes(getContentType(index));
</script>

<template>
  <div ref="root" class="table-view-wrapper">
    <ViewTitle :title="settings?.title" />
    <TableUIWithAutoSizeCalculation
      v-if="rows.loaded && numberOfDisplayedColumns > 0"
      ref="table"
      :class="{ 'will-change-scroll-position': enableWillChangeScrollPosition }"
      :data="[rowData]"
      :bottom-data="bottomRowData"
      :current-selection="selection ? [selection.top] : undefined"
      :current-bottom-selection="selection?.bottom"
      :total-selected="selection?.totalSelected"
      :data-config="dataConfig"
      :row-column-keys="rowColumnKeys"
      :row-column-sizes="rowColumnSizes"
      :row-column-formatters="rowColumnFormatters"
      :row-column-class-generators="rowColumnClassGenerators"
      :row-column-has-slot-content="rowColumnHasSlotContent"
      :row-column-has-data-value-view="rowColumnHasDataValueView"
      :row-column-no-padding="rowColumnNoPadding"
      :row-column-no-padding-left="rowColumnNoPaddingLeft"
      :row-column-clickables="rowColumnClickables"
      :table-config="tableConfig"
      :num-rows-above="rows.numRowsAbove"
      :num-rows-below="rows.numRowsBelow"
      :auto-column-sizes-options="autoColumnSizesOptions"
      :auto-row-height-options="autoRowHeightOptions"
      @page-change="(arg: any) => $emit('pageChange', arg)"
      @column-sort="
        (colIndex: number) =>
          $emit('columnSort', colIndex, getColumnId(colIndex))
      "
      @row-select="
        (...args: [any, any, any, any]) => $emit('rowSelect', ...args)
      "
      @select-all="(...args: [any]) => $emit('selectAll', ...args)"
      @search="(...args: [any]) => $emit('search', ...args)"
      @column-filter="
        (colIndex: number, newVal: any) =>
          $emit('columnFilter', getColumnId(colIndex), newVal)
      "
      @clear-filter="() => $emit('clearFilter')"
      @lazyload="(...args: [any]) => $emit('lazyload', ...args)"
      @column-resize="
        (colIndex: number, newSize: number) =>
          onColumnResize(getColumnId(colIndex), newSize)
      "
      @column-resize-start="columnResizeActive.setTrue"
      @column-resize-end="columnResizeActive.setFalse"
      @all-columns-resize="onAllColumnsResize"
      @update:available-width="onUpdateAvailableWidth"
      @header-sub-menu-item-selection="
        (item: any, colIndex: number) =>
          $emit(
            'headerSubMenuItemSelection',
            item,
            getColumnId(colIndex) as string,
          )
      "
      @auto-column-sizes-update="onAutoColumnSizesUpdate"
      @auto-row-height-update="$emit('rowHeightUpdate', $event)"
      @row-height-update="$emit('rowHeightUpdate', $event)"
      @ready="onTableIsReady"
      @copy-selection="onCopySelection"
      @data-value-view="onDataValueView"
      @close-data-value-view="onCloseDataValueView"
    >
      <template
        v-for="index in numberOfUsedColumns"
        :key="index"
        #[`cellContent-${index}`]="{
          data: { cell, width, height, paddingTopBottom },
          usedForAutoSizeCalculation,
        }"
      >
        <ImageRenderer
          v-if="getContentType(index) === 'img_path'"
          :key="index"
          :include-data-in-html="includeImageResources"
          :path="cell"
          :width="width"
          :height="
            typeof height === 'number' ? height - BORDER_BOTTOM_WIDTH : height
          "
          :base-url="baseUrl"
          :update="!columnResizeActive.state"
          :table-is-ready="tableIsReady"
          @pending="(id: string) => $emit('pendingImage', id)"
          @rendered="(id: string) => $emit('renderedImage', id)"
        />
        <HtmlRenderer
          v-else-if="getContentType(index) === 'html'"
          :content="cell"
          :padding-top-bottom="paddingTopBottom"
          :used-for-auto-size-calculation="usedForAutoSizeCalculation"
        />
        <MultiLineTextRenderer
          v-else-if="getContentType(index) === 'multi_line_txt'"
          :text="cell"
          :padding-top-bottom="paddingTopBottom"
        />
        <!-- @vue-expect-error getContentType can only be xml or json due to v-else-if -->
        <CodeRenderer
          v-else-if="useCodeRenderer(index)"
          :content="cell"
          :language="getContentType(index)"
        />
      </template>
      <template #subHeader="{ subHeader }">
        <SubHeaderTypeRenderer
          :data-types="header.dataTypes"
          :sub-header="subHeader"
        />
      </template>
    </TableUIWithAutoSizeCalculation>
    <EmptyDataState
      v-else
      :is-data-loaded="rows.loaded"
      :loading-animation-enabled
    />
  </div>
</template>

<style lang="postcss" scoped>
.table-view-wrapper {
  display: flex;
  flex-direction: column;
  max-height: inherit;
  min-height: inherit;

  & :deep(.row) {
    border-bottom: v-bind(BORDER_BOTTOM_WIDTH + "px") solid
      var(--knime-porcelain);
    align-content: center;
  }

  & :deep(.will-change-scroll-position > table > .container) {
    will-change: scroll-position;
  }
}
</style>
