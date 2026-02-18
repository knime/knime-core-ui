import type { ColumnType } from "@knime/knime-ui-table";

import type { TableCreatorParameters } from "../types";
import { getNextDefaultColumnName } from "../utils/columnNaming";

export const useAppend = ({
  defaultColumnType,
  getData,
  setAdjusted,
  onAppendColumn,
  onAppendRow,
}: {
  defaultColumnType: ColumnType;
  getData: () => TableCreatorParameters;
  setAdjusted: () => void;
  onAppendColumn?: () => void;
  onAppendRow?: () => void;
}) => {
  const appendColumn = () => {
    const data = getData();
    data.columns.push({
      name: getNextDefaultColumnName(data.columns.map((col) => col.name)),
      type: defaultColumnType,
      values: [],
    });
    onAppendColumn?.();
    setAdjusted();
  };

  const appendRow = () => {
    const data = getData();
    for (const col of data.columns) {
      col.values.push(null);
    }
    onAppendRow?.();
    setAdjusted();
  };

  return {
    appendColumn,
    appendRow,
  };
};
