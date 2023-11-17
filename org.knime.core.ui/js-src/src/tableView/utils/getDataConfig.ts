const isImage = (contentType: string) => contentType === "img_path";
const isHtml = (contentType: string) => contentType === "html";
const isMultiLineTxt = (contentType: string) =>
  contentType === "multi_line_txt";

import { getCustomRowHeight } from "../composables/useRowHeight";
import type TableViewViewSettings from "../types/ViewSettings";
import { RowHeightMode } from "../types/ViewSettings";
import specialColumns from "./specialColumns";
const { INDEX, ROW_ID, SKIPPED_REMAINING_COLUMNS_COLUMN } = specialColumns;

export default ({
  settings,
  displayedColumns,
  columnSizes,
  columnFiltersMap,
  columnContentTypes,
  colNameSelectedRendererId,
  dataTypes,
  columnDataTypeIds,
  columnFormatterDescriptions,
  columnNamesColors,
  indicateRemainingColumnsSkipped,
  enableRowResizing,
  enableDynamicRowHeight,
}: {
  settings: TableViewViewSettings;
  columnSizes: number[];
  columnFiltersMap?: Map<symbol | string, any>;
  displayedColumns: string[];
  columnContentTypes: string[];
  dataTypes: Record<
    string,
    {
      name: string;
      renderers: { name: string; id: string }[];
    }
  >;
  colNameSelectedRendererId?: Record<string, string>;
  columnDataTypeIds: any;
  columnFormatterDescriptions?: (string | null)[];
  columnNamesColors: string[] | null;
  indicateRemainingColumnsSkipped: any;
  enableRowResizing: boolean;
  enableDynamicRowHeight: boolean;
}) => {
  const {
    showRowKeys,
    showRowIndices,
    rowHeightMode,
    customRowHeight,
    showColumnDataType,
    enableRendererSelection,
  } = settings;

  const createHeaderSubMenuItems = (columnName: string, renderers: any[]) => {
    const headerSubMenuItems = [];
    headerSubMenuItems.push({
      text: "Data renderer",
      separator: true,
      sectionHeadline: true,
    });
    renderers.forEach((renderer) => {
      headerSubMenuItems.push({
        text: renderer.name,
        title: renderer.name,
        id: renderer.id,
        section: "dataRendering",
        selected: colNameSelectedRendererId
          ? colNameSelectedRendererId[columnName] === renderer.id
          : false,
      });
    });
    return headerSubMenuItems;
  };

  const createColumnConfig = ({
    id,
    index,
    columnName,
    filterConfig,
    columnTypeName,
    contentType,
    isSortable,
    columnTypeRenderers,
    headerColor,
  }: {
    id: symbol | string;
    index: number;
    columnName: string;
    filterConfig?: any;
    isSortable: boolean;
    columnTypeName?: string;
    contentType?: any;
    columnTypeRenderers?: any;
    headerColor?: string | null;
  }) => ({
    // the id is used to keep track of added/removed columns in the TableUIForAutoSizeCalculation
    id,
    // the key is used to access the data in the TableUI
    key: index,
    header: columnName,
    subHeader: columnTypeName,
    hasSlotContent:
      isImage(contentType) ||
      isHtml(contentType) ||
      isMultiLineTxt(contentType),
    size: columnSizes[index],
    filterConfig: filterConfig || { is: "", modelValue: "" },
    ...(columnTypeRenderers && {
      headerSubMenuItems: createHeaderSubMenuItems(
        columnName,
        columnTypeRenderers,
      ),
    }),
    formatter: (val: string) => val,
    isSortable,
    headerColor: headerColor ?? null,
  });

  const columnConfigs = [];
  if (showRowIndices) {
    columnConfigs.push(
      createColumnConfig({
        id: INDEX.id,
        index: 0,
        columnName: INDEX.name,
        isSortable: false,
      }),
    );
  }
  if (showRowKeys) {
    columnConfigs.push(
      createColumnConfig({
        id: ROW_ID.id,
        index: 1,
        columnName: ROW_ID.name,
        filterConfig: columnFiltersMap?.get(ROW_ID.id),
        isSortable: true,
      }),
    );
  }
  displayedColumns.forEach((columnName: string, index: number) => {
    const columnFormatterDescription = columnFormatterDescriptions?.[index];
    const renderers = dataTypes[columnDataTypeIds?.[index]]?.renderers as
      | any[]
      | undefined;
    // + 2: offset for the index and rowKey, because the first column
    // (index 0) always contains the indices and the second one the row keys
    const columnInformation = {
      id: columnName,
      index: index + 2,
      columnName,
      filterConfig: columnFiltersMap?.get(columnName),
      contentType: columnContentTypes?.[index],
      ...(showColumnDataType && {
        columnTypeName: dataTypes[columnDataTypeIds?.[index]]?.name,
      }),
      ...(enableRendererSelection && {
        columnTypeRenderers: renderers && [
          ...(columnFormatterDescription
            ? [{ id: null, name: columnFormatterDescription }]
            : []),
          ...(renderers || []),
        ],
      }),
      isSortable: true,
      headerColor: columnNamesColors?.[index],
    };
    columnConfigs.push(createColumnConfig(columnInformation));
  });
  if (indicateRemainingColumnsSkipped) {
    columnConfigs.push(
      createColumnConfig({
        id: SKIPPED_REMAINING_COLUMNS_COLUMN.id,
        index: displayedColumns.length + 2,
        columnName: SKIPPED_REMAINING_COLUMNS_COLUMN.name,
        isSortable: false,
      }),
    );
  }
  const compactMode = rowHeightMode === RowHeightMode.COMPACT;
  const customMode = rowHeightMode === RowHeightMode.CUSTOM;
  const defaultMode = rowHeightMode === RowHeightMode.DEFAULT;
  return {
    columnConfigs,
    rowConfig: {
      ...(customMode && { rowHeight: getCustomRowHeight({ customRowHeight }) }),
      ...(defaultMode &&
        enableDynamicRowHeight && { rowHeight: "dynamic" as const }),
      compactMode,
      enableResizing: enableRowResizing,
    },
  };
};
