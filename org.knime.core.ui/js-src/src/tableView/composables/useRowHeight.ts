import { constants } from "@knime/knime-ui-table";
import { ref } from "vue";
import type TableViewViewSettings from "../types/ViewSettings";
import { RowHeightMode } from "../types/ViewSettings";

type RelevantViewSettings = Pick<
  TableViewViewSettings,
  "customRowHeight" | "rowHeightMode"
>;

export const getInitialRowHeight = ({
  rowHeightMode,
  customRowHeight,
}: RelevantViewSettings) => {
  switch (rowHeightMode) {
    case RowHeightMode.COMPACT:
      return constants.COMPACT_ROW_HEIGHT;
    case RowHeightMode.CUSTOM:
      return customRowHeight;
    default:
      return constants.DEFAULT_ROW_HEIGHT;
  }
};

export default () => {
  const currentRowHeight = ref(constants.DEFAULT_ROW_HEIGHT);

  const setRowHeightSettings = (settings: RelevantViewSettings) => {
    currentRowHeight.value = getInitialRowHeight(settings);
  };

  const onRowHeightChange = (rowHeight: number) => {
    currentRowHeight.value = rowHeight;
  };

  return { currentRowHeight, setRowHeightSettings, onRowHeightChange };
};
