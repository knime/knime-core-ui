export const useDeleteCut = ({
  deleteCellValue,
  onCopySelection,
}: {
  deleteCellValue: (colIndex: number, rowIndex: number) => void;
  onCopySelection: (params: {
    rect: { x: { min: number; max: number }; y: { min: number; max: number } };
    withHeaders: boolean;
  }) => Promise<void>;
}) => {
  const onDeleteSelection = ({
    rect: { x, y },
  }: {
    rect: { x: { min: number; max: number }; y: { min: number; max: number } };
  }) => {
    for (let colIndex = x.min; colIndex <= x.max; colIndex++) {
      for (let rowIndex = y.min; rowIndex <= y.max; rowIndex++) {
        deleteCellValue(colIndex, rowIndex);
      }
    }
  };

  const onCutSelection = async ({
    rect,
  }: {
    rect: { x: { min: number; max: number }; y: { min: number; max: number } };
  }) => {
    await onCopySelection({ rect, withHeaders: false });
    onDeleteSelection({ rect });
  };

  return {
    onDeleteSelection,
    onCutSelection,
  };
};
