import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";

vi.mock("../../rpc", () => ({
  useRpcService: vi.fn(),
}));

import { useRpcService } from "../../rpc";
import { useValidation } from "../useValidation";

const createRpcMock = () => ({
  validateCellFromStringValue:
    vi.fn<(dataType: string, value: string) => Promise<boolean>>(),
  validateCellsFromStringValues:
    vi.fn<(dataType: string, values: string[]) => Promise<boolean[]>>(),
});

const setup = () => {
  const rpcMock = createRpcMock();
  vi.mocked(useRpcService).mockReturnValue(rpcMock);
  const onValidityUpdate = vi.fn();
  const validation = useValidation({ onValidityUpdate, cacheSize: 2 });
  return { validation, onValidityUpdate, rpcMock };
};

describe("useValidation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("setInitialDimensions", () => {
    it("creates identifiers for rows and columns", () => {
      const { validation, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 2, numColumns: 3 });

      // Validate a cell at each corner to verify identifiers exist
      validation.validateCell("IntType", "1", 0, 0);
      validation.validateCell("IntType", "2", 2, 1);

      expect(rpcMock.validateCellFromStringValue).toHaveBeenCalledTimes(2);
    });
  });

  describe("validateCell", () => {
    it("calls backend and invokes callback with index-based coordinates", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 2, numColumns: 2 });
      validation.validateCell("IntType", "42", 1, 0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(1, 0, true, "42");
      });
    });

    it("reports invalid values", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(false);

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.validateCell("IntType", "abc", 0, 0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, false, "abc");
      });
    });

    it("does nothing when column index is out of range", () => {
      const { validation, rpcMock } = setup();

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.validateCell("IntType", "1", 5, 0);

      expect(rpcMock.validateCellFromStringValue).not.toHaveBeenCalled();
    });

    it("does nothing when row index is out of range", () => {
      const { validation, rpcMock } = setup();

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.validateCell("IntType", "1", 0, 5);

      expect(rpcMock.validateCellFromStringValue).not.toHaveBeenCalled();
    });
  });

  describe("validateColumn", () => {
    it("calls backend and invokes callback for each value", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellsFromStringValues.mockResolvedValue([true, false]);

      validation.setInitialDimensions({ numRows: 2, numColumns: 1 });
      validation.validateColumn("IntType", ["1", "abc"], 0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, true, "1");
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 1, false, "abc");
      });
    });

    it("skips null values in validation", async () => {
      const { validation, rpcMock } = setup();
      rpcMock.validateCellsFromStringValues.mockResolvedValue([true]);

      validation.setInitialDimensions({ numRows: 3, numColumns: 1 });
      validation.validateColumn("IntType", ["1", null, "2"], 0);

      await vi.waitFor(() => {
        expect(rpcMock.validateCellsFromStringValues).toHaveBeenCalledWith(
          "IntType",
          ["1", "2"],
        );
      });
    });

    it("accepts explicit rowIndices", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellsFromStringValues.mockResolvedValue([true]);

      validation.setInitialDimensions({ numRows: 5, numColumns: 1 });
      validation.validateColumn("IntType", ["val"], 0, [3]);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 3, true, "val");
      });
    });

    it("does nothing when column index is out of range", () => {
      const { validation, rpcMock } = setup();

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.validateColumn("IntType", ["1"], 5);

      expect(rpcMock.validateCellsFromStringValues).not.toHaveBeenCalled();
    });
  });

  describe("validateArea", () => {
    it("validates multiple columns", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellsFromStringValues
        .mockResolvedValueOnce([true])
        .mockResolvedValueOnce([false]);

      validation.setInitialDimensions({ numRows: 1, numColumns: 2 });
      validation.validateArea([
        { dataType: "IntType", values: ["1"], columnIndex: 0, rowIndices: [0] },
        {
          dataType: "StringType",
          values: ["x"],
          columnIndex: 1,
          rowIndices: [0],
        },
      ]);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, true, "1");
        expect(onValidityUpdate).toHaveBeenCalledWith(1, 0, false, "x");
      });
    });
  });

  describe("index stability after deletion", () => {
    it("adjusts callback indices after a row is deleted", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 3, numColumns: 1 });

      validation.validateCell("IntType", "val", 0, 2);
      // Delete row at index 0 — row that was at index 2 is now at index 1
      validation.deleteRow(0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 1, true, "val");
      });
    });

    it("adjusts callback indices after a column is deleted", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 1, numColumns: 3 });

      validation.validateCell("IntType", "val", 2, 0);
      // Delete column at index 0 — column that was at index 2 is now at index 1
      validation.deleteColumn(0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(1, 0, true, "val");
      });
    });

    it("does not invoke callback for a deleted row", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();

      // Use a deferred promise so we can delete the row before resolution
      let resolveValidation!: (value: boolean) => void;
      rpcMock.validateCellFromStringValue.mockReturnValue(
        new Promise((resolve) => {
          resolveValidation = resolve;
        }),
      );

      validation.setInitialDimensions({ numRows: 2, numColumns: 1 });
      validation.validateCell("IntType", "val", 0, 0);

      // Delete the row before the validation resolves
      validation.deleteRow(0);
      resolveValidation(true);

      // Give microtasks a chance to flush
      await nextTick();

      expect(onValidityUpdate).not.toHaveBeenCalled();
    });

    it("does not invoke callback for a deleted column", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();

      let resolveValidation!: (value: boolean) => void;
      rpcMock.validateCellFromStringValue.mockReturnValue(
        new Promise((resolve) => {
          resolveValidation = resolve;
        }),
      );

      validation.setInitialDimensions({ numRows: 1, numColumns: 2 });
      validation.validateCell("IntType", "val", 0, 0);

      validation.deleteColumn(0);
      resolveValidation(true);

      await nextTick();

      expect(onValidityUpdate).not.toHaveBeenCalled();
    });
  });

  describe("append", () => {
    it("new rows can be validated after appending", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.appendNewRow();
      validation.validateCell("IntType", "new", 0, 1);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 1, true, "new");
      });
    });

    it("new columns can be validated after appending", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 1, numColumns: 1 });
      validation.appendNewColumn();
      validation.validateCell("IntType", "new", 1, 0);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(1, 0, true, "new");
      });
    });
  });

  describe("caching", () => {
    it("does not make a second backend call for the same dataType+value", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 2, numColumns: 1 });

      validation.validateCell("IntType", "42", 0, 0);
      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, true, "42");
      });

      onValidityUpdate.mockClear();
      validation.validateCell("IntType", "42", 0, 1);

      // Should immediately resolve from cache
      expect(onValidityUpdate).toHaveBeenCalledWith(0, 1, true, "42");
      expect(rpcMock.validateCellFromStringValue).toHaveBeenCalledTimes(1);
    });

    it("waits for pending validation to resolve and uses its result for a callback", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();

      let resolveValidation!: (value: boolean) => void;
      rpcMock.validateCellFromStringValue.mockReturnValue(
        new Promise((resolve) => {
          resolveValidation = resolve;
        }),
      );

      validation.setInitialDimensions({ numRows: 2, numColumns: 1 });

      validation.validateCell("IntType", "42", 0, 0);

      // Before resolving the first validation, call validateCell again with the same value
      validation.validateCell("IntType", "42", 0, 1);

      // Resolve the validation
      resolveValidation(true);

      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, true, "42");
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 1, true, "42");
      });
    });

    it("makes separate backend calls for different dataTypes", () => {
      const { validation, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 1, numColumns: 2 });

      validation.validateCell("IntType", "42", 0, 0);
      validation.validateCell("DoubleType", "42", 1, 0);

      expect(rpcMock.validateCellFromStringValue).toHaveBeenCalledTimes(2);
    });

    it("clears old cache entries when cache size is exceeded", async () => {
      const { validation, onValidityUpdate, rpcMock } = setup();
      rpcMock.validateCellFromStringValue.mockResolvedValue(true);

      validation.setInitialDimensions({ numRows: 1, numColumns: 4 });

      validation.validateCell("IntType", "val1", 0, 0);
      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(0, 0, true, "val1");
      });
      validation.validateCell("IntType", "val2", 1, 0);
      validation.validateCell("IntType", "val3", 2, 0);
      await vi.waitFor(() => {
        expect(onValidityUpdate).toHaveBeenCalledWith(1, 0, true, "val2");
        expect(onValidityUpdate).toHaveBeenCalledWith(2, 0, true, "val3");
      });

      validation.validateCell("IntType", "val1", 3, 0);

      // Cache size is 2, so "val1" (oldest) was evicted when "val3" was added
      expect(rpcMock.validateCellFromStringValue).toHaveBeenCalledTimes(4);
    });
  });
});
