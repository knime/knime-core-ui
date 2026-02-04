<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref, useTemplateRef } from "vue";

import { InputField, Label, NumberInput, SplitPanel } from "@knime/components";
import {
  DialogService,
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";
import {
  TableUI,
  type DataConfig,
  type TableConfig,
} from "@knime/knime-ui-table";

import NodeDialogCore from "../nodeDialog/NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "../nodeDialog/api/types/RpcTypes";
import type { NodeDialogCoreInitialData } from "../nodeDialog/types/InitialData";
import useFlowVariableSystem from "../nodeDialog/composables/useFlowVariableSystem";
import { defaultRenderers, JsonFormsDialog } from "@knime/jsonforms";
import SubHeaderTypeRendererBase from "@/tableView/components/SubHeaderTypeRendererBase.vue";
import type { JsonSchema } from "@jsonforms/core";
import CellInput from "./CellInput.vue";

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

// Display mode tracking
const displayMode = ref<"small" | "large">("small");
const isLargeMode = computed(() => displayMode.value === "large");

// Right pane state management
const rightPaneExpanded = ref(true);
const rightPaneSize = ref<number>(300);

// NodeDialogCore
const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const dialogInitialData = ref<NodeDialogCoreInitialData | null>(null);
const dataTypeIdToHashIdMap = ref<Record<string, string>>({});

let dialogService: DialogService;

// Minimal RPC method implementation for demo purposes
const callRpcMethod: NodeDialogCoreRpcMethods = async (method, options) => {
  console.log("RPC method called:", method, options);
  // Return minimal responses for common methods
  return {} as any;
};

/**
 * Index of the column whose context menu is currently active. -1 if none.
 */
const activeColumnIndex = ref<number>(0);

// Extract columns from dialog initial data
const columns = computed(() => {
  const cols = dialogInitialData.value?.data?.model?.columns;
  return Array.isArray(cols) ? cols : [];
});

const unknownDataType = { id: "unknown-datatype", text: "Unknown datatype" };

// Helper to get type display name from schema choices
const getTypeIdAndText = (typeId: string | undefined) => {
  if (!typeId || !dialogInitialData.value) {
    return unknownDataType;
  }

  const possibleValues = dialogInitialData.value.initialUpdates![0].values[0].value as {id: string; type: {
  id: string;
    text: string;
  }}[];
  return possibleValues.find(
    (item: any) => item.id === typeId
  )?.type ?? unknownDataType;
};

const getTypeIdByText = (typeText: string) => {
  if (!dialogInitialData.value) {
    return "unknown-datatype";
  }

  const possibleValues = dialogInitialData.value.initialUpdates![0].values[0].value as {id: string; type: {
  id: string;
    text: string;
  }}[];
  const found = possibleValues.find(
    (item: any) => item.type.text === typeText
  );
  return found ? found.id : "unknown-datatype";
};

// Data configuration computed from columns
const dataConfig = computed<DataConfig>(() => {
  const cols = columns.value;

  const columnConfigs = cols.map((col, index) => ({
    header: col?.name ?? "",
    subHeader: col?.type,
    size: 150,
    type: col?.type ?? "?",
    key: `col${index}`,
    id: `col${index}`,
    formatter: (value: any) => (value !== undefined && value !== null ? String(value) : ""),
  }));

  return {
    columnConfigs,
    rowConfig: {
      rowHeight: 40,
    },
  };
});

// Table data computed from columns
// TableUI expects an array of groups, where each group is an array of row objects
const tableData = computed(() => {
  const cols = columns.value;

  if (cols.length === 0) {
    return [[]];
  }

  // Find the maximum number of rows across all columns
  const numRows = Math.max(0, ...cols.map(col => (col?.values ?? []).length));
  const rows: any[] = [];

  for (let rowIndex = 0; rowIndex < numRows; rowIndex++) {
    const row: any = {};
    cols.forEach((col, colIndex) => {
      const values = col?.values ?? [];
      row[`col${colIndex}`] = values[rowIndex] ?? null;
    });
    rows.push(row);
  }

  return [rows]; // Wrap in array for single group
});

// Table configuration computed to update page config automatically
const tableConfig = computed<TableConfig>(() => {
  const cols = columns.value;
  const numRows = Math.max(0, ...cols.map(col => (col?.values ?? []).length));

  return {
    showSelection: false,
    showCollapser: false,
    showPopovers: false,
    showColumnFilters: false,
    showBottomControls: false,
    subMenuItems: [],
    groupSubMenuItems: [],
    enableCellSelection: true,
    enableHeaderCellSelection: true,
    enableEditingCells: true,
    enableVirtualScrolling: true,
    showNewColumnAndRowButton: true,
    enableColumnResizing: false,
    pageConfig: {
      currentSize: numRows,
      tableSize: numRows,
      pageSize: numRows,
      currentPage: 1,
      columnCount: cols.length,
      showTableSize: true,
      showPageControls: false,
    },
  };
});

// Flow variable system
const { setInitialFlowVariablesMap} = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: () => coreComponent.value?.getCurrentData() ?? {},
});

// Handle cell selection copy (without backend call)
const onCopySelection = async ({
  rect: { x, y },
  id,
  withHeaders,
}: {
  rect: { x: { min: number; max: number }; y: { min: number; max: number } };
  id: null | number;
  withHeaders: boolean;
}) => {
  document.body.style.cursor = "wait";

  try {
    const group = tableData.value[id ? 0 : 0]; // Only one group in our case
    const selectedRows = group.slice(y.min, y.max + 1);
    const columnKeys = dataConfig.value.columnConfigs.map((col) => col.key);
    const selectedColumnKeys = columnKeys.slice(x.min, x.max + 1);
    const headers = dataConfig.value.columnConfigs
      .slice(x.min, x.max + 1)
      .map((col) => col.header);
      
      // Build CSV content
      let csvContent = "";
    if (withHeaders) {
      csvContent += headers.join("\t") + "\n";
    }
    selectedRows.forEach((row) => {
      const rowValues = selectedColumnKeys.map((key) => {
        const value = row[key as string];
        return value !== undefined && value !== null ? String(value) : "";
      });
      csvContent += rowValues.join("\t") + "\n";
    });

    // Build HTML content
    let htmlContent = "<table>";
    if (withHeaders) {
      htmlContent += "<thead><tr>";
      headers.forEach((header) => {
        htmlContent += `<th>${header}</th>`;
      });
      htmlContent += "</tr></thead>";
    }
    htmlContent += "<tbody>";
    selectedRows.forEach((row) => {
      htmlContent += "<tr>";
      selectedColumnKeys.forEach((key) => {
        const value = row[key as string];
        const displayValue = value !== undefined && value !== null ? String(value) : "";
        htmlContent += `<td>${displayValue}</td>`;
      });
      htmlContent += "</tr>";
    });
    htmlContent += "</tbody></table>";

    // Copy to clipboard
    const blobHTML = new Blob([htmlContent], { type: "text/html" });
    const blobCSV = new Blob([csvContent], { type: "text/plain" });

    const clipboardItem = new ClipboardItem({
      [blobHTML.type]: blobHTML,
      [blobCSV.type]: blobCSV,
    });

    await navigator.clipboard.write([clipboardItem]);
    console.log("Copied selection to clipboard");
  } catch (error) {
    console.error("Failed to copy content to clipboard:", error);
  }

  document.body.style.cursor = "unset";
};

// Handle paste into table
const onPasteSelection = async ({
  rect: { x, y },
  id,
  updateSelection,
}: {
  rect: { x: { min: number; max: number }; y: { min: number; max: number } };
  id: null | number;
  updateSelection: (newRect: { minX: number; minY: number; maxX: number; maxY: number }) => void;
}) => {
  try {
    // Read from clipboard
    const clipboardText = await navigator.clipboard.readText();
    if (!clipboardText) {
      return;
    }

    // Parse the pasted data (TSV format)
    const clipboardRows = clipboardText.split('\n');
    if (clipboardRows.length > 0 && clipboardRows[clipboardRows.length - 1] === '') {
      clipboardRows.pop(); // Remove trailing empty line if present
    }
    const rows = clipboardRows.map(row => row.split('\t'));

    if (rows.length === 0 || !dialogInitialData.value?.data?.model?.columns) {
      return;
    }

    const cols = dialogInitialData.value.data.model.columns;
    const numCols = rows[0].length;
    const numRows = rows.length;

    // Create new columns if pasting beyond existing columns
    const neededColumns = x.min + numCols;
    while (cols.length < neededColumns) {
      cols.push({
        name: "Column " + (cols.length + 1),
        type: getTypeIdByText("String"),
        values: [],
      });
    }

    // Update column values starting from the selection position
    for (let colOffset = 0; colOffset < numCols; colOffset++) {
      const colIndex = x.min + colOffset;
      const column = cols[colIndex];

      if (!column.values) {
        column.values = [];
      }

      for (let rowOffset = 0; rowOffset < numRows; rowOffset++) {
        const rowIndex = y.min + rowOffset;
        const value = rows[rowOffset][colOffset];

        // Extend values array if needed
        while (column.values.length <= rowIndex) {
          column.values.push(null);
        }

        column.values[rowIndex] = value;
      }
    }

    // Update the selection to cover the pasted area
    const newMaxX = x.min + numCols - 1;
    const newMaxY = y.min + numRows - 1;


    updateSelection({
      minX: x.min,
      minY: y.min,
      maxX: newMaxX,
      maxY: newMaxY,
    });

    console.log(`Pasted ${numRows}x${numCols} cells`);
  } catch (error) {
    console.error("Failed to paste content:", error);
  }
};

onMounted(async () => {
  const service = getKnimeService();
  dialogService = new DialogService(service);
  displayMode.value = dialogService.getInitialDisplayMode();
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });

  // Get and log initial data
  const jsonDataService = new JsonDataService(service);
  const initialData = await jsonDataService.initialData();
  console.log("Initial data:", initialData);

  // Set dialog initial data if available
  if (initialData) {
    dialogInitialData.value = JSON.parse(initialData.settingsInitialData) as NodeDialogCoreInitialData;
    dataTypeIdToHashIdMap.value = initialData.dataTypeIdToHashIdMap || {};


    // Initialize flow variables map
    if (dialogInitialData.value.flowVariableSettings) {
      setInitialFlowVariablesMap(dialogInitialData.value.flowVariableSettings);
    }
  }

  dialogService.setApplyListener(() => 
    jsonDataService.applyData({
      data: dialogInitialData.value?.data,
      flowVariableSettings: dialogInitialData.value?.flowVariableSettings,
    })
  )

});

const onChange = ({data}) => {
}
const setStateProviderListener = (...listenerArgs: any) => {
  console.log("State provider listener set:", listenerArgs);
};


const tableRef = "tableRef";
const tableComponent = useTemplateRef<typeof TableUI>(tableRef);



const addNewRow = (lastPosition: { columnInd: number, rectId: number | null } | null) => {
  const cols = dialogInitialData.value?.data?.model?.columns;
  if (cols && Array.isArray(cols)) {
    cols.forEach((col) => {
      if (!col.values) {
        col.values = [];
      }
      col.values.push(null); // Add empty string as new row value
    });
    console.log("Added new row to", cols.length, "columns");
  }
  const colInd = lastPosition?.columnInd ?? 0;
  tableComponent.value?.updateCellSelection({
    minX: colInd,
    minY: tableData.value[0].length - 1,
    maxX: colInd,
    maxY: tableData.value[0].length - 1,
},
lastPosition?.rectId);

};

const addNewColumn = async () => {
  const cols = dialogInitialData.value?.data?.model?.columns;
  if (cols && Array.isArray(cols)) {
    const newColIndex = cols.length;
    cols.push({
      name: "Column " + (newColIndex + 1),
      type: getTypeIdByText("String"),
      values: [],
    });
    await nextTick();
    tableComponent.value?.focusHeaderCell(newColIndex);
  }
};
</script>

<template>
  <div class="table-creator-dialog">
    <SplitPanel
      v-model:expanded="rightPaneExpanded"
      v-model:secondary-size="rightPaneSize"
      :hide-secondary-pane="!isLargeMode"
      direction="right"
      :secondary-snap-size="220"
      use-pixel
      keep-element-on-close
      class="split-panel"
    >
      <div class="main-content">
        <NodeDialogCore
            v-if="displayMode === 'small' && dialogInitialData"
            ref="coreComponent"
            :initial-data="dialogInitialData"
            :has-node-view="false"
            :call-rpc-method="callRpcMethod"
            :register-settings="dialogService.registerSettings.bind(dialogService)"
          />

          <TableUI
            :ref="tableRef"
            :data="tableData"
            :data-config="dataConfig"
            :table-config="tableConfig"
            @copy-selection="onCopySelection"
            @paste-selection="onPasteSelection"
            @cell-selection-change="(cellPosition) => activeColumnIndex = cellPosition?.x ?? -1"
            @new-row-button-click="addNewRow($event)"
            @new-column-button-click="addNewColumn()"
          >
           <template #subHeader="{ subHeader }">
        <SubHeaderTypeRendererBase
            :icon-name="getTypeIdAndText(subHeader).id"
            :data-type-name="getTypeIdAndText(subHeader ).text"
        />
      </template>
      <template #editable-cell="{ initialValue, rowInd, colInd, onKeydown, onClickAway, cellElement}">
        <CellInput 
           :initial-value="initialValue"
           :model-value="tableData[0][rowInd]?.['col' + colInd]"
           :reference-element="cellElement"
           @update:model-value="(val) => {
              dialogInitialData!.data.model!.columns[colInd].values[rowInd] = val;
           }"
           @keydown="onKeydown"
           @click-away="onClickAway"
        />
      </template>
      </TableUI>

        <div v-if="!isLargeMode" class="small-mode-hint">
          <p>
            💡 Enlarge the dialog to see additional options in the right panel
          </p>
        </div>
      </div>

      <!-- Right panel - only visible in large mode -->
      <template #secondary>
        <div v-if="dialogInitialData && activeColumnIndex !== -1" class="right-pane">
          <JsonFormsDialog
            :data="dialogInitialData.data.model!.columns[activeColumnIndex]"
            :schema="dialogInitialData.schema.properties!.model.properties!.columns.items as JsonSchema"
            :uischema="{elements: dialogInitialData.ui_schema.elements[0].options.detail}"
            @change="({data}) => {
              dialogInitialData!.data.model!.columns[activeColumnIndex] = data;
            }"
            :renderers="defaultRenderers"
            @state-provider-listener="(_id, callback) => callback(dialogInitialData?.initialUpdates[0].values[0].value)"
          />
        </div>
      </template>
    </SplitPanel>
  </div>
</template>

<style scoped>
.table-creator-dialog {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.split-panel {
  height: 100%;
  width: 100%;
  flex-grow: 1;
}

.main-content {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.small-mode-hint {
  padding: 15px;
  background-color: var(--knime-yellow-light, #fff9c4);
  border-left: 4px solid var(--knime-yellow, #fdd835);
  border-radius: 4px;
  margin-top: 20px;
}

.small-mode-hint p {
  margin: 0;
}

.right-pane {
  background-color: var(--knime-gray-ultra-light, #fafafa);
  height: 100%;
  overflow-y: auto;
  border-left: 1px solid var(--knime-gray-light, #e0e0e0);
}

.loading {
  padding: 20px;
  text-align: center;
  color: var(--knime-masala, #666);
}
</style>
