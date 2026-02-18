import { describe, expect, it } from "vitest";

import { getNextDefaultColumnName, getUniqueColumnName } from "../columnNaming";

describe("columnNaming", () => {
  describe("getUniqueColumnName", () => {
    it("returns the suggested name if it doesn't exist", () => {
      const existingNames = ["Column 1", "Column 2"];
      expect(getUniqueColumnName("Column 3", existingNames)).toBe("Column 3");
    });

    it("trims the suggested name", () => {
      const existingNames = ["Column 1"];
      expect(getUniqueColumnName("  Column 3  ", existingNames)).toBe(
        "Column 3",
      );
    });

    it("adds (#1) suffix if name exists", () => {
      const existingNames = ["My Column", "Column 2"];
      expect(getUniqueColumnName("My Column", existingNames)).toBe(
        "My Column (#1)",
      );
    });

    it("increments suffix if base name with (#N) already exists", () => {
      const existingNames = ["My Column", "My Column (#1)", "My Column (#2)"];
      expect(getUniqueColumnName("My Column", existingNames)).toBe(
        "My Column (#3)",
      );
    });

    it("extracts base name and increments from existing (#N) pattern", () => {
      const existingNames = ["Data", "Data (#1)", "Data (#2)", "Data (#4)"];
      expect(getUniqueColumnName("Data (#2)", existingNames)).toBe("Data (#3)");
    });

    it("finds first available number in sequence", () => {
      const existingNames = ["Test", "Test (#1)", "Test (#3)"];
      expect(getUniqueColumnName("Test", existingNames)).toBe("Test (#2)");
    });

    it("handles names with spaces correctly", () => {
      const existingNames = ["Column Name", "Column Name (#1)"];
      expect(getUniqueColumnName("Column Name", existingNames)).toBe(
        "Column Name (#2)",
      );
    });

    it("works with Set instead of array", () => {
      const existingNames = new Set(["Column 1", "Column 2"]);
      expect(getUniqueColumnName("Column 1", existingNames)).toBe(
        "Column 1 (#1)",
      );
    });

    it("handles edge case with pattern at end of name", () => {
      const existingNames = ["Name (#5)"];
      expect(getUniqueColumnName("Name (#5)", existingNames)).toBe("Name (#6)");
    });

    it("handles very large numbers in pattern", () => {
      const existingNames = ["Col", "Col (#999999999)"];
      expect(getUniqueColumnName("Col (#999999999)", existingNames)).toBe(
        "Col (#1000000000)",
      );
    });

    it("picks next default name for empty or blank name", () => {
      const existingNames = ["Column 1", "Column 3"];
      expect(getUniqueColumnName("", existingNames)).toBe("Column 2");
      expect(getUniqueColumnName("   ", existingNames)).toBe("Column 2");
    });
  });

  describe("getNextDefaultColumnName", () => {
    it("returns Column 1 for empty list", () => {
      expect(getNextDefaultColumnName([])).toBe("Column 1");
    });

    it("returns Column 1 if it doesn't exist", () => {
      const existingNames = ["Column 2", "Column 3"];
      expect(getNextDefaultColumnName(existingNames)).toBe("Column 1");
    });

    it("returns Column 2 if Column 1 exists", () => {
      const existingNames = ["Column 1"];
      expect(getNextDefaultColumnName(existingNames)).toBe("Column 2");
    });

    it("finds first available column number", () => {
      const existingNames = ["Column 1", "Column 2", "Column 4"];
      expect(getNextDefaultColumnName(existingNames)).toBe("Column 3");
    });

    it("works with Set instead of array", () => {
      const existingNames = new Set(["Column 1", "Column 2"]);
      expect(getNextDefaultColumnName(existingNames)).toBe("Column 3");
    });

    it("handles non-sequential column names", () => {
      const existingNames = ["Custom Name", "Column 1", "Another Column"];
      expect(getNextDefaultColumnName(existingNames)).toBe("Column 2");
    });
  });
});
