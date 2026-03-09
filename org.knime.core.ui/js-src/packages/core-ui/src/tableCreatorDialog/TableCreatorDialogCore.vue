<script setup lang="ts">
import { nextTick, onMounted } from "vue";

import { TableUI } from "@knime/knime-ui-table";

import SubHeaderTypeRendererBase from "@/tableView/components/SubHeaderTypeRendererBase.vue";

import TableCreatorLayout from "./TableCreatorLayout.vue";
import CellInput from "./components/CellInput.vue";
import CellInputFloating from "./components/CellInputFloating.vue";
import ColumnHeaderInput from "./components/ColumnHeaderInput.vue";
import TableCell from "./components/TableCell.vue";
import { useAppend } from "./composables/useAppend";
import { useCellData } from "./composables/useCellData";
import { useCopyPaste } from "./composables/useCopyPaste";
import { useDataType } from "./composables/useDataType";
import { useDeleteCut } from "./composables/useDeleteCut";
import { useDeletion } from "./composables/useDeletion";
import { useSelection } from "./composables/useSelection";
import { useTableConfig } from "./composables/useTableConfig";
import { useValidation } from "./composables/useValidation";
import type { InitialData } from "./types";
import { getOtherColumnNames } from "./utils/columnNaming";

const emit = defineEmits<{
  adjusted: [];
}>();

const setAdjusted = () => emit("adjusted");

const { dialogInitialData } = defineProps<{
  dialogInitialData: InitialData;
  isLargeMode: boolean;
}>();

const { getColumnDataType, getTypeIdAndText } = useDataType({
  dialogInitialData,
});

const validator = useValidation({
  onValidityUpdate: (colIdx, rowIdx, isValid, value) => {
    const cols = dialogInitialData.data.model.columns;
    if (!cols || !cols[colIdx]) {
      return;
    }
    const col = cols[colIdx];
    if (!col.values) {
      return;
    }
    if (col.values[rowIdx] === value) {
      if (!col.isInvalidAt) {
        col.isInvalidAt = [];
      }
      col.isInvalidAt[rowIdx] = !isValid;
    }
  },
});

onMounted(() =>
  validator.setInitialDimensions({
    numRows: dialogInitialData.data.model.numRows,
    numColumns: dialogInitialData.data.model.columns.length,
  }),
);

const deleter = useDeletion({
  getData: () => dialogInitialData.data.model,
  setAdjusted,
  onDeleteColumn: validator.deleteColumn,
  onDeleteRow: (rowIndex: number) => {
    validator.deleteRow(rowIndex);
    dialogInitialData.data.model.numRows--; // eslint-disable-line vue/no-mutating-props
  },
});

const appender = useAppend({
  defaultColumnType:
    dialogInitialData.schema.properties.model.properties.columns.items
      .properties.type.default,
  getData: () => dialogInitialData.data.model,
  setAdjusted,
  onAppendColumn: validator.appendNewColumn,
  onAppendRow: () => {
    validator.appendNewRow();
    dialogInitialData.data.model.numRows++; // eslint-disable-line vue/no-mutating-props
  },
});

const {
  setCellValue,
  getCellValue,
  setColumnName,
  setColumnType,
  getColumnParams,
  setCellArea,
} = useCellData({
  dialogInitialData,
  getColumnDataType,
  validator,
  setAdjusted,
});

const getNumRows = () => dialogInitialData.data.model.numRows;
const getNumColumns = () => dialogInitialData.data.model.columns.length;

const tableRef = "tableComponent";
const tableCreatorLayoutRef = "tableCreatorLayout";
const columnHeaderInputRef = "columnHeaderInput";
const {
  selectedColumnIndex,
  selectedRowIndex,
  onCellPositionChange,
  uniquifyColumnNames,
  refocusTable,
  onHeaderCellStartEditing,
  focusHeaderCell,
  focusCell,
  selectFirstCellInColumn,
} = useSelection({
  tableRef,
  tableCreatorLayoutRef,
  columnHeaderInputRef,
  getColumns: () => dialogInitialData.data.model.columns,
  getNumRows,
  setAdjusted,
});

const { tableData, dataConfig, tableConfig } = useTableConfig({
  dialogInitialData,
  getCellValue,
  getNumRows,
});

const { onCopySelection, onPasteSelection } = useCopyPaste({
  getNumRows,
  getNumColumns,
  appendColumn: appender.appendColumn,
  appendRow: appender.appendRow,
  setCellArea,
  getCellValue,
  getColumnName: (colIndex) =>
    dialogInitialData.data.model.columns[colIndex].name,
});

const { onDeleteSelection, onCutSelection } = useDeleteCut({
  onCopySelection,
  deleteCellValue: (colIndex, rowIndex) =>
    setCellValue(colIndex, rowIndex, null),
});

const addNewColumn = () => {
  appender.appendColumn();
  nextTick().then(() => focusHeaderCell(getNumColumns() - 1));
};

const addNewRow = (
  lastPosition: { columnInd: number; rectId: number | null } | null,
) => {
  appender.appendRow();
  if (getNumColumns() > 0) {
    nextTick().then(() =>
      focusCell(
        lastPosition?.columnInd ?? 0,
        getNumRows() - 1,
        lastPosition?.rectId,
      ),
    );
  }
};
</script>

<template>
  <div class="table-creator-dialog" tabindex="-1">
    <TableCreatorLayout
      :ref="tableCreatorLayoutRef"
      :is-large-mode
      @refocus-table="refocusTable"
    >
      <template #main-table>
        <div class="main-content">
          <TableUI
            :ref="tableRef"
            :data="tableData"
            :data-config="dataConfig"
            :table-config="tableConfig"
            @copy-selection="onCopySelection"
            @paste-selection="onPasteSelection"
            @cut-selection="onCutSelection"
            @delete-selection="onDeleteSelection"
            @cell-selection-change="onCellPositionChange"
            @new-row-button-click="addNewRow($event)"
            @new-column-button-click="addNewColumn()"
            @header-cell-start-editing="onHeaderCellStartEditing"
            @delete-row="deleter.deleteRow($event)"
            @delete-column="deleter.deleteColumn($event)"
          >
            <template
              v-for="config in dataConfig.columnConfigs"
              :key="config.key"
              #[`cellContent-${String(config.key)}`]="{
                data: { cell, paddingTopBottom },
              }"
            >
              <TableCell
                :value="cell.value"
                :is-invalid="!cell.isValid"
                :padding-top-bottom="paddingTopBottom"
              />
            </template>
            <template #subHeader="{ subHeader }">
              <SubHeaderTypeRendererBase
                :icon-name="getTypeIdAndText(subHeader).id"
                :data-type-name="getTypeIdAndText(subHeader).text"
              />
            </template>
            <template
              #editable-cell="{
                initialValue,
                rowInd,
                colInd,
                onKeydown,
                onClickAway,
                cellElement,
              }"
            >
              <CellInputFloating
                :key="`cell(${colInd}:${rowInd})`"
                :initial-value="initialValue"
                :model-value="getCellValue(colInd, rowInd)"
                :row-ind
                :col-ind
                :reference-element="cellElement"
                @update:model-value="setCellValue(colInd, rowInd, $event)"
                @keydown="onKeydown"
                @click-away="onClickAway"
              />
            </template>
          </TableUI>
        </div>
      </template>
      <template #side-panel="{ close }">
        <div class="right-pane" @keydown.escape.prevent.stop="close">
          <!-- Focus sentinel: when the user tabs past the side panel content,
               focus returns to the table so keyboard navigation stays inside
               the table creator in large (side-by-side) mode. -->
          <div
            v-if="isLargeMode && selectedColumnIndex >= 0"
            :tabindex="0"
            @focus="refocusTable"
          />
          <CellInput
            v-if="selectedRowIndex >= 0 && selectedColumnIndex >= 0"
            class="cell-input"
            :model-value="getCellValue(selectedColumnIndex, selectedRowIndex)"
            @update:model-value="
              setCellValue(selectedColumnIndex, selectedRowIndex, $event)
            "
          />
          <ColumnHeaderInput
            v-else-if="
              selectedColumnIndex >= 0 &&
              selectedColumnIndex < dialogInitialData.data.model.columns.length
            "
            :ref="columnHeaderInputRef"
            :column-data="getColumnParams(selectedColumnIndex)"
            :data-type-possible-values="
              dialogInitialData.initialUpdates[0].values[0].value
            "
            :other-column-names="
              getOtherColumnNames(
                dialogInitialData.data.model.columns.map((col) => col.name),
                selectedColumnIndex,
              )
            "
            @update:column-name="setColumnName(selectedColumnIndex, $event)"
            @update:column-type="setColumnType(selectedColumnIndex, $event)"
            @column-name-keydown-enter="
              [close(), selectFirstCellInColumn(selectedColumnIndex)]
            "
            @column-name-focus-out="uniquifyColumnNames"
          />
          <div
            v-if="isLargeMode && selectedColumnIndex >= 0"
            :tabindex="0"
            @focus="refocusTable"
          />
        </div>
      </template>
    </TableCreatorLayout>
  </div>
</template>

<style scoped>
.table-creator-dialog {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  padding: 20px;
  height: 100%;
}

.right-pane {
  background-color: var(--knime-gray-ultra-light);
  height: 100%;
  width: 100%;
  border-left: 1px solid var(--knime-gray-light);

  & .cell-input {
    padding: var(--space-16);
  }
}
</style>
