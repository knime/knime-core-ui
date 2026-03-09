import { ref } from "vue";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import type { StringOrEnum } from "../types/StringOrEnum";

export type TitleColumn = StringOrEnum<"NONE" | "ROW_ID">;

export type ColorColumn = StringOrEnum<"NONE">;

export interface TileViewSettings {
  title: string;
  displayedColumns: { selected?: string[] };
  titleColumn: TitleColumn;
  colorColumn: ColorColumn;
  tilesPerRow: number;
  displayColumnHeaders: boolean;
  textAlignment: "LEFT" | "CENTER" | "RIGHT";
  pageSize: number;
  selectionMode: SelectionMode;
  showOnlySelectedRows: boolean;
  showOnlySelectedRowsConfigurable: boolean;
}

export const DEFAULT_SETTINGS: TileViewSettings = {
  title: "",
  displayedColumns: { selected: [] },
  titleColumn: { specialChoice: "ROW_ID" },
  colorColumn: { specialChoice: "NONE" },
  tilesPerRow: 3,
  displayColumnHeaders: true,
  textAlignment: "LEFT",
  pageSize: 10,
  selectionMode: SelectionMode.EDIT,
  showOnlySelectedRows: false,
  showOnlySelectedRowsConfigurable: false,
};

const arraysEqual = (a: unknown[], b: unknown[]) => {
  if (a === b) {
    return true;
  }
  if (a.length !== b.length) {
    return false;
  }
  return a.every((val, i) => val === b[i]);
};

const stringOrEnumEqual = (
  a: { specialChoice?: string; regularChoice?: string | null },
  b: { specialChoice?: string; regularChoice?: string | null },
) => {
  if (a === b) {
    return true;
  }
  return (
    ("specialChoice" in a &&
      "specialChoice" in b &&
      a.specialChoice === b.specialChoice) ||
    ("regularChoice" in a &&
      "regularChoice" in b &&
      a.regularChoice === b.regularChoice)
  );
};

export interface SettingsDiff {
  /** Whether any setting change requires re-fetching table data. */
  needsRefetch: boolean;
  /** Whether the page should be reset to 0 before re-fetching. */
  needsPageReset: boolean;
  /** Whether the set of displayed columns changed (server-side cache must be invalidated). */
  displayedColumnsChanged: boolean;
}

/**
 * Owns the `settings` ref and provides utilities to compute what changed
 * between two settings objects and to apply a new settings value.
 * Fetching is intentionally NOT done here — callers use `fetchTable` directly.
 */
export const useSettings = () => {
  const settings = ref<TileViewSettings>(DEFAULT_SETTINGS);

  const updateSettings = (
    prevSettings: TileViewSettings,
    nextSettings: TileViewSettings,
  ): SettingsDiff => {
    const displayedColumnsChanged = !arraysEqual(
      nextSettings.displayedColumns.selected ?? [],
      prevSettings.displayedColumns.selected ?? [],
    );
    const titleColumnChanged = !stringOrEnumEqual(
      nextSettings.titleColumn,
      prevSettings.titleColumn,
    );
    const colorColumnChanged = !stringOrEnumEqual(
      nextSettings.colorColumn,
      prevSettings.colorColumn,
    );
    const pageSizeChanged = nextSettings.pageSize !== prevSettings.pageSize;
    const showOnlySelectedRowsChanged =
      nextSettings.showOnlySelectedRows !== prevSettings.showOnlySelectedRows;

    const diff: SettingsDiff = {
      needsRefetch:
        displayedColumnsChanged ||
        titleColumnChanged ||
        colorColumnChanged ||
        pageSizeChanged ||
        showOnlySelectedRowsChanged,
      needsPageReset: pageSizeChanged || showOnlySelectedRowsChanged,
      displayedColumnsChanged,
    };

    settings.value = nextSettings;
    return diff;
  };

  return { settings, updateSettings };
};
