/* eslint-disable no-magic-numbers */
import { describe, expect, it } from "vitest";

import {
  combineDataPathsWithIndices,
  getIndicesFromDataPaths,
  scopeToDataPaths,
} from "../dataPaths";

describe("dataPaths", () => {
  describe("scopeToDataPaths", () => {
    it.each([
      { scope: "#/properties/foo", expected: ["foo"] },
      { scope: "#/properties/foo/properties/bar", expected: ["foo.bar"] },
      {
        scope: "#/properties/foo/items/properties/bar",
        expected: ["foo", "bar"],
      },
      {
        scope: "#/properties/foo/items/properties/bar/properties/baz",
        expected: ["foo", "bar.baz"],
      },
      {
        scope: "#/properties/foo/properties/items/properties/bar",
        expected: ["foo.items.bar"],
      },
      { scope: "#/properties/foo/properties/123", expected: ["foo.123"] },
      {
        scope: "#/properties/foo/items/properties/test2/properties/123",
        expected: ["foo", "test2.123"],
      },
      {
        scope: "#/properties/foo/properties/items/properties/123",
        expected: ["foo.items.123"],
      },
      {
        scope: "#/properties/properties/properties/properties",
        expected: ["properties.properties"],
      },
      {
        scope: "#/properties/foo/items/properties/bar/items/properties/baz",
        expected: ["foo", "bar", "baz"],
      },
      { scope: "#/properties/items", expected: ["items"] },
      {
        scope: "#/properties/items/properties/items",
        expected: ["items.items"],
      },
      { scope: "#/properties/foo/properties/items", expected: ["foo.items"] },
      {
        scope: "#/properties/items/items/properties/items",
        expected: ["items", "items"],
      },
      {
        scope:
          "#/properties/items/items/properties/items/properties/items/properties/leaf",
        expected: ["items", "items.items.leaf"],
      },
      {
        scope: "#/properties/arrayField/items/properties/itemsField",
        expected: ["arrayField", "itemsField"],
      },
    ] as const)(
      "should convert scope %s to data paths",
      ({ scope, expected }) => {
        const result = scopeToDataPaths(scope);
        expect(result).toEqual(expected);
      },
    );
  });

  describe("combineDataPathsWithIndices", () => {
    it("should combine data paths with enough indices", () => {
      const dataPaths = ["foo", "bar", "baz"];
      const indices = [1, 2];
      const result = combineDataPathsWithIndices(dataPaths, indices);
      expect(result).toEqual(["foo.1.bar.2.baz"]);
    });

    it("should combine data paths with partial indices", () => {
      const dataPaths = ["foo", "bar", "baz"];
      const indices = [1];
      const result = combineDataPathsWithIndices(dataPaths, indices);
      expect(result).toEqual(["foo.1.bar", "baz"]);
    });

    it("should combine data paths without indices", () => {
      const dataPaths = ["foo", "bar", "baz"];
      const indices: number[] = [];
      const result = combineDataPathsWithIndices(dataPaths, indices);
      expect(result).toEqual(["foo", "bar", "baz"]);
    });
  });

  describe("getIndicesFromDataPaths", () => {
    const splitPathAndGetIndicesFromDataPaths = (
      dataPaths: string[][],
      path: string,
    ) => getIndicesFromDataPaths(dataPaths, path.split("."))?.indices ?? null;

    it("matches indices from data paths", () => {
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem"]], "lorem"),
      ).toStrictEqual([]);
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem"], ["ipsum"]], "lorem"),
      ).toStrictEqual([]);
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem.ipsum"]], "lorem"),
      ).toStrictEqual([]);
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem"]], "lorem.ipsum"),
      ).toStrictEqual([]);
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem", "ipsum"]], "lorem.123"),
      ).toStrictEqual([123]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum"]],
          "lorem.123.ipsum",
        ),
      ).toStrictEqual([123]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem.ipsum", "dolor"]],
          "lorem.ipsum.123.dolor",
        ),
      ).toStrictEqual([123]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum.dolor"]],
          "lorem.123.ipsum",
        ),
      ).toStrictEqual([123]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum", "dolor"]],
          "lorem.123.ipsum.45",
        ),
      ).toStrictEqual([123, 45]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum", "dolor"]],
          "lorem.123.ipsum.45.dolor",
        ),
      ).toStrictEqual([123, 45]);
    });

    it("returns null if not matched", () => {
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem"]], "ipsum"),
      ).toBeNull();
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem"]], "ipsum.lorem"),
      ).toBeNull();
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem", "ipsum"]], "lorem.123."),
      ).toBeNull();
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum"]],
          "lorem.123.dolor.456.ipsum",
        ),
      ).toBeNull();
      expect(
        splitPathAndGetIndicesFromDataPaths([["loremipsum"]], "lorem"),
      ).toBeNull();
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum", "dolor"]],
          "lorem.123",
        ),
      ).toBeNull();
    });

    /**
     * E.g. when an array element is added, a value trigger inside of the array should not trigger.
     */
    it("return null if matched but the number of indices in the output would be smaller than required", () => {
      expect(
        splitPathAndGetIndicesFromDataPaths([["lorem", "ipsum"]], "lorem"),
      ).toBeNull();

      expect(
        splitPathAndGetIndicesFromDataPaths(
          [["lorem", "ipsum", "dolor"]],
          "lorem.123.ipsum",
        ),
      ).toBeNull();
    });

    it("decides for the longer indices array in case multiple data paths match", () => {
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [
            ["lorem", "ipsum", "dolor"],
            ["lorem", "ipsum"],
          ],
          "lorem.123.ipsum.45.dolor",
        ),
      ).toStrictEqual([123, 45]);
      expect(
        splitPathAndGetIndicesFromDataPaths(
          [
            ["lorem", "ipsum"],
            ["lorem", "ipsum", "dolor"],
          ],
          "lorem.123.ipsum.45.dolor",
        ),
      ).toStrictEqual([123, 45]);
    });
  });
});
