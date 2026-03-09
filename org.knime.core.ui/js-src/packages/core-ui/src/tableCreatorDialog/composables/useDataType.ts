import type { InitialData } from "../types";

const unknownDataType = { id: "unknown-datatype", text: "Unknown datatype" };

export const useDataType = ({
  dialogInitialData,
}: {
  dialogInitialData: InitialData;
}) => {
  /**
   * Gets the data type for a column by its index.
   * Returns the column's type property which corresponds to the data type ID.
   */
  const getColumnDataType = (colIndex: number): string | null => {
    const cols = dialogInitialData.data.model.columns;
    if (!cols || !cols[colIndex]) {
      return null;
    }
    return cols[colIndex].type ?? null;
  };

  const getTypeIdAndText = (
    typeId: string | undefined,
  ): { id: string; text: string } => {
    if (!typeId) {
      return unknownDataType;
    }

    const possibleValues = dialogInitialData.initialUpdates[0].values[0]
      .value as {
      id: string;
      type: {
        id: string;
        text: string;
      };
    }[];
    return (
      possibleValues.find((item: any) => item.id === typeId)?.type ??
      unknownDataType
    );
  };

  return {
    getColumnDataType,
    getTypeIdAndText,
  };
};
