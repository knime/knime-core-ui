import { computed } from "vue";

import {
  getMissingValueMessage,
  isMissingValue,
  unpackObjectRepresentation,
} from "@knime/knime-ui-table";

import type { Cell } from "@/tableView/types/Table";
import { SelectionMode } from "@/tableView/types/ViewSettings";

import type { TileViewSettings } from "./useSettings";

interface TileProps {
  row: (Cell | string | null)[];
  title: Cell | string | null;
  showTitle: boolean;
  textAlignment: TileViewSettings["textAlignment"];
  selectionMode: SelectionMode;
  isReport: boolean;
}

const unpackCellValue = (cell: Cell | string | null) => {
  const isMissing = isMissingValue(cell);
  const value = isMissing
    ? getMissingValueMessage(cell)
    : unpackObjectRepresentation(cell);
  return { value, isMissing };
};

export const useTile = (props: TileProps) => {
  const transformedRow = computed(() =>
    props.row.map((cell) => unpackCellValue(cell)),
  );

  const textAlign = computed(
    () => props.textAlignment.toLowerCase() as "left" | "center" | "right",
  );

  const iconAlign = computed(() => {
    if (props.textAlignment === "CENTER") {
      return "center";
    }
    return props.textAlignment === "RIGHT" ? "flex-end" : "flex-start";
  });

  const titleCell = computed(() => unpackCellValue(props.title));

  const showSelection = computed(
    () => props.selectionMode !== SelectionMode.OFF && !props.isReport,
  );

  const enableSelection = computed(
    () => props.selectionMode === SelectionMode.EDIT,
  );

  // title row + one row per data column (skipping row index and row id) + optional footer row
  const rowSpan = computed(
    () =>
      (props.showTitle ? 1 : 0) +
      props.row.length -
      2 +
      (showSelection.value ? 1 : 0),
  );

  return {
    transformedRow,
    textAlign,
    iconAlign,
    titleCell,
    showSelection,
    enableSelection,
    rowSpan,
  };
};
