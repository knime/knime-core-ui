import { describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { type VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { TableUI } from "@knime/knime-ui-table";

import TableCreatorDialogCore from "../TableCreatorDialogCore.vue";
import CellInput from "../components/CellInput.vue";
import CellInputFloating from "../components/CellInputFloating.vue";
import ColumnHeaderInput from "../components/ColumnHeaderInput.vue";
import ColumnNameInput from "../components/ColumnNameInput.vue";
import ColumnTypeInput from "../components/ColumnTypeInput.vue";
import type { TableCreatorRpcMethods } from "../rpc";
import type { InitialData } from "../types";

// Polyfill ResizeObserver for jsdom
globalThis.ResizeObserver = class {
  observe() {
    // No-op
  }

  unobserve() {
    // No-op
  }

  disconnect() {
    // No-op
  }
} as any;

// Mock the RPC service used by useValidation
vi.mock("../rpc", () => ({
  useRpcService: () =>
    ({
      validateCellFromStringValue: (dataType: string, value: string) => {
        if (dataType === "IntValue" && !/^-?\d+$/.test(value)) {
          return Promise.resolve(false);
        }
        return Promise.resolve(true);
      },
      validateCellsFromStringValues: (dataType: string, values: string[]) => {
        if (dataType === "IntValue") {
          const results = values.map((value) => /^-?\d+$/.test(value));
          return Promise.resolve(results);
        }
        const results = values.map(() => true);
        return Promise.resolve(results);
      },
    }) satisfies TableCreatorRpcMethods,
}));

const createInitialData = (
  overrides: {
    columns?: { name: string; type: string; values: (string | null)[] }[];
    numRows?: number;
  } = {},
): InitialData => {
  const columns = overrides.columns ?? [
    { name: "Column 0", type: "StringValue", values: ["a", "b", "c"] },
    { name: "Column 1", type: "IntValue", values: ["1", "2", "3"] },
    { name: "Column 2", type: "DoubleValue", values: [null, "2.2", "3.3"] },
  ];
  const numRows = overrides.numRows ?? columns[0]?.values.length ?? 0;

  const uiSchemaKey = "ui_schema";

  return {
    data: { model: { numRows, columns } },
    schema: {
      properties: {
        model: {
          properties: {
            columns: {
              items: {
                properties: {
                  name: { type: "string" },
                  type: {
                    type: "string",
                    default: "StringValue",
                  },
                },
              },
            },
          },
        },
      },
    },
    [uiSchemaKey]: {
      elements: [
        {
          options: {
            detail: [
              {
                scope:
                  "#/properties/model/properties/columns/items/properties/name",
              },
              {
                scope:
                  "#/properties/model/properties/columns/items/properties/type",
                options: {
                  format: "dropDown",
                },
                providedOptions: ["possibleValues"],
              },
            ],
          },
        },
      ],
    },
    initialUpdates: [
      {
        values: [
          {
            value: [
              {
                id: "StringValue",
                text: "String",
                type: { id: "StringValue", text: "String" },
              },
              {
                id: "IntValue",
                text: "Integer",
                type: { id: "IntValue", text: "Integer" },
              },
              {
                id: "DoubleValue",
                text: "Double",
                type: { id: "DoubleValue", text: "Double" },
              },
            ],
          },
        ],
      },
    ],
    persist: {},
  } as any;
};

// --- Mount helper ---

const mountTableCreatorDialogCore = async ({
  initialData,
  isLargeMode = true,
}: { initialData?: InitialData; isLargeMode?: boolean } = {}) => {
  const data = initialData ?? createInitialData();
  const wrapper = mount(TableCreatorDialogCore, {
    props: { dialogInitialData: data, isLargeMode },
    attachTo: document.body, // Attach to document.body to enable focus-related tests
  });
  await flushPromises();
  return { wrapper };
};

const getTableUIComponent = (wrapper: VueWrapper<any>) => {
  return wrapper.findComponent(TableUI);
};

const getTableData = (wrapper: VueWrapper<any>) => {
  return getTableUIComponent(wrapper).props("data")!;
};

const getColumnConfigs = (wrapper: VueWrapper<any>) => {
  return getTableUIComponent(wrapper).props("dataConfig")!.columnConfigs;
};

const getTdWithText = (wrapper: VueWrapper<any>, text: string) => {
  return wrapper.findAll("td").filter((td) => td.text() === text)[0];
};

const getThWithText = (wrapper: VueWrapper<any>, text: string) => {
  return wrapper.findAll("th").filter((th) => th.text().includes(text))[0];
};

// --- Tests ---

describe("TableCreatorDialogCore", () => {
  it("mounts and sets the correct table data", async () => {
    const { wrapper } = await mountTableCreatorDialogCore();
    expect(getTableData(wrapper).length).toBe(1);
    expect(getTableData(wrapper)[0].length).toBe(3);
    expect(getTableData(wrapper)[0][0]).toStrictEqual({
      col0: expect.objectContaining({
        value: "a",
      }),
      col1: expect.objectContaining({
        value: "1",
      }),
      col2: null,
    });
  });

  describe("cell editing", () => {
    it("shows floating cell editor", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      getTdWithText(wrapper, "a").trigger("pointerdown", { button: 0 });
      getTdWithText(wrapper, "a").trigger("pointerup", { button: 0 });

      await flushPromises();
      const cellInputFloating = wrapper.findComponent(CellInputFloating);
      expect(cellInputFloating.exists()).toBeTruthy();
      expect(cellInputFloating.props("modelValue")?.value).toBe("a");

      cellInputFloating.vm.$emit("update:modelValue", {
        value: "new value",
        isValid: true,
      });

      const preventDefault = vi.fn();
      const stopPropagation = vi.fn();

      await cellInputFloating.vm.$emit("keydown", {
        key: "Enter",
        preventDefault,
        stopPropagation,
      });

      expect(preventDefault).toHaveBeenCalled();
      expect(stopPropagation).toHaveBeenCalled();

      expect(wrapper.findComponent(CellInputFloating).exists()).toBeFalsy();

      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col0: expect.objectContaining({
            value: "new value",
          }),
        }),
      );
    });

    it("validates cell input from floating editor", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      getTdWithText(wrapper, "1").trigger("pointerdown", { button: 0 });
      getTdWithText(wrapper, "1").trigger("pointerup", { button: 0 });

      await flushPromises();
      const cellInputFloating = wrapper.findComponent(CellInputFloating);
      expect(cellInputFloating.exists()).toBeTruthy();
      expect(cellInputFloating.props("modelValue")?.value).toBe("1");

      // Emit invalid value for IntValue column
      cellInputFloating.vm.$emit("update:modelValue", {
        value: "invalid",
        isValid: false,
      });

      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col1: {
            value: "invalid",
            isValid: false,
          },
        }),
      );
    });

    it("sets and validates cell input from side panel editor", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      getTdWithText(wrapper, "1").trigger("pointerdown", { button: 0 });
      getTdWithText(wrapper, "1").trigger("pointerup", { button: 0 });

      await flushPromises();
      const cellInput = wrapper.findComponent(CellInput);
      expect(cellInput.exists()).toBeTruthy();
      expect(cellInput.props("modelValue")?.value).toBe("1");

      // Change to valid value first:
      cellInput.vm.$emit("update:modelValue", {
        value: "42",
        isValid: true,
      });

      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col1: {
            value: "42",
            isValid: true,
          },
        }),
      );

      // Change to invalid value:
      cellInput.vm.$emit("update:modelValue", {
        value: "invalid",
        isValid: true,
      });

      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col1: {
            value: "invalid",
            isValid: false,
          },
        }),
      );
    });
  });

  const getHeaderCellElement = (
    wrapper: VueWrapper<any>,
    columnName: string,
  ) => {
    return getThWithText(wrapper, columnName).element.getElementsByClassName(
      "column-header-content",
    )[0] as HTMLDivElement;
  };

  describe("column editing", () => {
    const focusHeaderCell = async (
      wrapper: VueWrapper<any>,
      columnName: string,
    ) => {
      getHeaderCellElement(wrapper, columnName).dispatchEvent(
        new FocusEvent("focus"),
      );
      await nextTick();
    };

    it("shows column parameters editor when a header cell is focused", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();

      await focusHeaderCell(wrapper, "Column 1");

      expect(wrapper.findComponent(ColumnHeaderInput).exists()).toBeTruthy();
    });

    it("sets focus to column name input when clicking on header cell", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();

      const headerCellElement = getHeaderCellElement(wrapper, "Column 1");
      headerCellElement.dispatchEvent(new FocusEvent("focus"));
      await nextTick();

      const columnNameInput = wrapper.findComponent(ColumnNameInput);
      expect(columnNameInput.exists()).toBeTruthy();

      expect(document.activeElement).not.toBe(
        columnNameInput.element.querySelector("input"),
      );

      headerCellElement.dispatchEvent(new MouseEvent("click"));
      await nextTick();
      expect(document.activeElement).toBe(
        columnNameInput.element.querySelector("input"),
      );
    });

    it("reopens collapsed split panel when clicking on header cell", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();

      const splitPanel = wrapper.findComponent({ name: "SplitPanel" });
      expect(splitPanel.exists()).toBeTruthy();

      // Collapse the split panel
      splitPanel.vm.$emit("update:expanded", false);
      await nextTick();
      expect(splitPanel.classes()).toContain("closed");

      // Click on a header cell
      const headerCellElement = getHeaderCellElement(wrapper, "Column 1");
      headerCellElement.dispatchEvent(new FocusEvent("focus"));
      await nextTick();
      headerCellElement.dispatchEvent(new MouseEvent("click"));
      await nextTick();

      expect(splitPanel.classes()).not.toContain("closed");
    });

    it("does not show column parameters in small mode on mere focus", async () => {
      const { wrapper } = await mountTableCreatorDialogCore({
        isLargeMode: false,
      });

      await focusHeaderCell(wrapper, "Column 1");

      expect(wrapper.findComponent(ColumnHeaderInput).exists()).toBeFalsy();
    });

    it("shows and focusses column name input in small mode on click", async () => {
      const { wrapper } = await mountTableCreatorDialogCore({
        isLargeMode: false,
      });
      const headerCellElement = getHeaderCellElement(wrapper, "Column 1");
      headerCellElement.dispatchEvent(new FocusEvent("focus"));
      await nextTick();
      headerCellElement.dispatchEvent(new MouseEvent("click"));
      await nextTick();

      const columnNameInput = wrapper.findComponent(ColumnNameInput);
      expect(columnNameInput.exists()).toBeTruthy();
      expect(document.activeElement).toBe(
        columnNameInput.element.querySelector("input"),
      );
    });

    it("revalidates column when changing type", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      await focusHeaderCell(wrapper, "Column 0");

      const columnTypeInput = wrapper.findComponent(ColumnTypeInput);
      columnTypeInput.vm.$emit("update:modelValue", "IntValue");
      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col0: {
            value: "a",
            isValid: false, // "a" is not valid for IntValue type
          },
        }),
      );
    });
  });

  describe("deletions and appends", () => {
    it("deletes column when delete button is clicked on column header", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      const deleteButtons = wrapper.findAll(
        'button[aria-label="Delete column"]',
      );
      expect(deleteButtons).toHaveLength(3);
      const deleteButton = deleteButtons[1];
      expect(deleteButton.exists()).toBeTruthy();
      await deleteButton.trigger("click");
      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col0: expect.objectContaining({
            value: "a",
          }),
          col1: null,
        }),
      );
    });

    it("deletes row when delete button is clicked left of the row", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      const deleteButtons = wrapper.findAll('button[aria-label="Delete row"]');
      expect(deleteButtons).toHaveLength(3);
      const deleteButton = deleteButtons[1];
      await deleteButton.trigger("click");
      await flushPromises();
      await nextTick();
      expect(getTableData(wrapper)[0]).toHaveLength(2);
      expect(getTableData(wrapper)[0][1]).toStrictEqual(
        expect.objectContaining({
          col0: expect.objectContaining({
            value: "c",
          }),
          col1: expect.objectContaining({
            value: "3",
          }),
          col2: expect.objectContaining({
            value: "3.3",
          }),
        }),
      );
    });

    it("appends row when append row button is clicked", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      const appendRowButton = wrapper
        .findAll("button")
        .filter((btn) => btn.text() === "Add")[1];
      expect(appendRowButton).toBeDefined();
      await appendRowButton!.trigger("click");

      await flushPromises();
      expect(getTableData(wrapper)[0]).toHaveLength(4);
      expect(getTableData(wrapper)[0][3]).toStrictEqual(
        expect.objectContaining({
          col0: null,
          col1: null,
          col2: null,
        }),
      );
    });

    it("appends column when append column button is clicked on header", async () => {
      const { wrapper } = await mountTableCreatorDialogCore();
      const appendColumnButton = wrapper
        .findAll("button")
        .filter((btn) => btn.text() === "Add")[0];
      expect(appendColumnButton).toBeDefined();
      await appendColumnButton!.trigger("click");
      await flushPromises();
      expect(getTableData(wrapper)[0][0]).toStrictEqual(
        expect.objectContaining({
          col3: null,
        }),
      );
      const columnConfigs = getColumnConfigs(wrapper);
      expect(columnConfigs).toHaveLength(4);
      expect(columnConfigs[3]).toStrictEqual(
        expect.objectContaining({
          header: "Column 3",
          type: "StringValue",
        }),
      );
    });
  });
});
