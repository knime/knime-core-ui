import { describe, expect, it, vi } from "vitest";

import { useDeleteCut } from "../useDeleteCut";

describe("useDeleteCut", () => {
  const setup = () => {
    const deleteCellValue = vi.fn();
    const onCopySelection = vi.fn().mockResolvedValue(undefined);
    const { onDeleteSelection, onCutSelection } = useDeleteCut({
      deleteCellValue,
      onCopySelection,
    });
    return {
      deleteCellValue,
      onCopySelection,
      onDeleteSelection,
      onCutSelection,
    };
  };

  describe("onDeleteSelection", () => {
    it("deletes all cells in the given rectangle", () => {
      const { deleteCellValue, onDeleteSelection } = setup();
      onDeleteSelection({
        rect: { x: { min: 1, max: 2 }, y: { min: 0, max: 1 } },
      });
      expect(deleteCellValue).toHaveBeenCalledTimes(4);
      expect(deleteCellValue).toHaveBeenCalledWith(1, 0);
      expect(deleteCellValue).toHaveBeenCalledWith(1, 1);
      expect(deleteCellValue).toHaveBeenCalledWith(2, 0);
      expect(deleteCellValue).toHaveBeenCalledWith(2, 1);
    });
  });

  describe("onCutSelection", () => {
    it("copies selection then deletes it", async () => {
      const { deleteCellValue, onCopySelection, onCutSelection } = setup();
      const rect = { x: { min: 0, max: 0 }, y: { min: 0, max: 0 } };
      await onCutSelection({ rect });
      expect(onCopySelection).toHaveBeenCalledWith({
        rect,
        withHeaders: false,
      });
      expect(deleteCellValue).toHaveBeenCalledWith(0, 0);
    });
  });
});
