import { type Ref, computed, reactive, ref } from "vue";

import type { ColumnSizes, TableViewDisplayProps } from "../types";
import specialColumns from "../utils/specialColumns";

const { INDEX, ROW_ID, SKIPPED_REMAINING_COLUMNS_COLUMN } = specialColumns;

export interface UseColumnSizesOptions {
  header: Ref<
    Pick<
      TableViewDisplayProps["header"],
      "displayedColumns" | "indicateRemainingColumnsSkipped"
    >
  >;
  settings: Ref<
    Pick<TableViewDisplayProps["settings"], "showRowIndices" | "showRowKeys">
  >;
  autoColumnSizes: Ref<ColumnSizes>;
  autoColumnSizesActive: Ref<boolean>;
}

const DEFAULT_COLUMN_SIZE = 100;

const getDataColumnSizes = ({
  availableSpace,
  displayedColumns,
  columnSizeOverrides,
  defaultColumnSizeOverride,
}: {
  availableSpace: number;
  displayedColumns: string[];
  columnSizeOverrides: ColumnSizes;
  defaultColumnSizeOverride: number | null;
}) => {
  const defaultColumnSize = Math.max(
    DEFAULT_COLUMN_SIZE,
    availableSpace / displayedColumns.length,
  );
  const currentDefaultColumnSize =
    defaultColumnSizeOverride ?? defaultColumnSize;
  return displayedColumns.reduce((columnSizes, columnName) => {
    columnSizes.push(
      columnSizeOverrides[columnName] || currentDefaultColumnSize,
    );
    return columnSizes;
  }, [] as number[]);
};

const stretchOneColumnToFillAvailableSpace = ({
  columnSizes,
  index,
  availableSpace,
}: {
  columnSizes: number[];
  index: number;
  availableSpace: number;
}) => {
  const totalSize = columnSizes.reduce((sum, size) => sum + size, 0);
  if (totalSize < availableSpace) {
    columnSizes[index] += availableSpace - totalSize;
  }
  return columnSizes;
};

export default ({
  settings,
  header,
  autoColumnSizes,
  autoColumnSizesActive,
}: UseColumnSizesOptions) => {
  const columnSizeOverrides: ColumnSizes = reactive({});
  const defaultColumnSizeOverride: Ref<number | null> = ref(null);
  const availableWidth: Ref<number> = ref(0);

  const onColumnResize = (
    columnName: string | symbol,
    newColumnSize: number,
  ) => {
    columnSizeOverrides[columnName] = newColumnSize;
  };

  const onAllColumnsResize = (columnSize: number) => {
    defaultColumnSizeOverride.value = columnSize;
    header.value.displayedColumns.forEach((columnName) => {
      delete columnSizeOverrides[columnName];
    });
  };

  const deleteColumnSizeOverrides = (
    columnIdsToDelete: (string | symbol)[] | null = null,
  ) => {
    let allColumnIdsToDelete = columnIdsToDelete;
    if (allColumnIdsToDelete === null) {
      allColumnIdsToDelete = Reflect.ownKeys(columnSizeOverrides);
      defaultColumnSizeOverride.value = null;
    }
    allColumnIdsToDelete.forEach((columnId) => {
      delete columnSizeOverrides[columnId];
    });
  };

  const onUpdateAvailableWidth = (newAvailableWidth: number) => {
    if (newAvailableWidth && !autoColumnSizesActive.value) {
      // update all overridden column widths according to the relative change of the available width
      const ratio = newAvailableWidth / availableWidth.value;
      Reflect.ownKeys(columnSizeOverrides).forEach((key) => {
        columnSizeOverrides[key] *= ratio;
      });
      if (defaultColumnSizeOverride.value) {
        defaultColumnSizeOverride.value *= ratio;
      }
    }
    availableWidth.value = newAvailableWidth;
  };

  const initialIndexColumnSize = computed(() =>
    settings.value.showRowIndices ? INDEX.defaultSize : 0,
  );
  const initialRowKeyColumnSize = computed(() =>
    settings.value.showRowKeys ? ROW_ID.defaultSize : 0,
  );
  const initialRemainingSkippedColumnSize = computed(() =>
    header.value.indicateRemainingColumnsSkipped
      ? SKIPPED_REMAINING_COLUMNS_COLUMN.defaultSize
      : 0,
  );

  const initialTableColumnsSizeTotal = computed(
    () =>
      availableWidth.value -
      initialIndexColumnSize.value -
      initialRowKeyColumnSize.value -
      initialRemainingSkippedColumnSize.value,
  );

  const columnSizeOverridesOrAutoSizes = computed(() =>
    [
      INDEX.id,
      ROW_ID.id,
      ...header.value.displayedColumns,
      SKIPPED_REMAINING_COLUMNS_COLUMN.id,
    ].reduce((acc, columnName) => {
      acc[columnName] =
        columnSizeOverrides[columnName] || autoColumnSizes.value[columnName];
      return acc;
    }, {} as ColumnSizes),
  );

  const indexColumnSize = computed(
    () =>
      columnSizeOverridesOrAutoSizes.value[INDEX.id] ||
      initialIndexColumnSize.value,
  );
  const rowKeyColumnSize = computed(
    () =>
      columnSizeOverridesOrAutoSizes.value[ROW_ID.id] ||
      initialRowKeyColumnSize.value,
  );
  const remainingSkippedColumnSize = computed(
    () =>
      columnSizeOverridesOrAutoSizes.value[
        SKIPPED_REMAINING_COLUMNS_COLUMN.id
      ] || initialRemainingSkippedColumnSize.value,
  );

  const columnSizes = computed(() => {
    const currentColumnSizes = [indexColumnSize.value, rowKeyColumnSize.value]
      .concat(
        getDataColumnSizes({
          availableSpace: initialTableColumnsSizeTotal.value,
          displayedColumns: header.value.displayedColumns,
          columnSizeOverrides: columnSizeOverridesOrAutoSizes.value,
          defaultColumnSizeOverride: defaultColumnSizeOverride.value,
        }),
      )
      .concat(
        header.value.indicateRemainingColumnsSkipped
          ? [remainingSkippedColumnSize.value]
          : [],
      );

    const indexColumnIsOnlyColumn =
      !settings.value.showRowKeys && currentColumnSizes.length === 2;
    const lastColumnIndex = indexColumnIsOnlyColumn
      ? 0
      : currentColumnSizes.length - 1;
    return stretchOneColumnToFillAvailableSpace({
      columnSizes: currentColumnSizes,
      index: lastColumnIndex,
      availableSpace: availableWidth.value,
    });
  });

  return {
    columnSizes,
    onColumnResize,
    onAllColumnsResize,
    onUpdateAvailableWidth,
    deleteColumnSizeOverrides,
  };
};
