import { ref } from "vue";
import type { Ref } from "vue";

import type { CachingSelectionService } from "@knime/ui-extension-service";

import type { TileViewSettings } from "./useSettings";
import type { FetchTableOptions, TileViewTableData } from "./useTableData";

export const useSelection = (
  selectionService: CachingSelectionService,
  table: Ref<TileViewTableData>,
  settings: Ref<TileViewSettings>,
  currentPage: Ref<number>,
  fetchTable: (overrides?: Partial<FetchTableOptions>) => Promise<void>,
) => {
  const selection = ref<boolean[]>([]);

  const transformSelection = () => {
    const currentSelection = selectionService.getCachedSelection();
    selection.value = table.value.rows
      .map((row) => row[1])
      .map((rowId) => typeof rowId === "string" && currentSelection.has(rowId));
  };

  const onSelectionChange = async () => {
    if (settings.value.showOnlySelectedRows) {
      currentPage.value = 0;
      await fetchTable({ clearImageDataCache: true, fromIndex: 0 });
    }
    transformSelection();
  };

  const updateSelection = (rowId: string, selected: boolean) => {
    selectionService[selected ? "add" : "remove"]([rowId]);
    transformSelection();
  };

  return { selection, transformSelection, onSelectionChange, updateSelection };
};
