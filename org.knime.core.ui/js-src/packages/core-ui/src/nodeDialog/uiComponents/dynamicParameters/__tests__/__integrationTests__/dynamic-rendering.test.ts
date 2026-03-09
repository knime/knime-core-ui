import { beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";

import { TextControl } from "@knime/jsonforms";

import {
  createDynamicInputInitialData,
  mockInitialUpdate,
  mountNodeDialog,
  setupDynamicInputTest,
} from "./utils";

describe("dynamic parameters - dynamic rendering", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders dynamic UI after initial update", async () => {
    const { wrapper } = await setupDynamicInputTest({
      dynamicData: { key: "dynamicValue" },
      dynamicSchema: {
        type: "object",
        properties: {
          key: { type: "string", title: "Dynamic Key" },
        },
      },
      dynamicUiSchema: {
        elements: [
          {
            scope: "#/properties/key",
            type: "Control",
          },
        ],
      },
    });

    await flushPromises();

    // Should render the dynamic input
    const textControls = wrapper.findAllComponents(TextControl);
    expect(textControls.length).toBeGreaterThan(0);

    // Find the label for the dynamic input
    const labels = wrapper.findAll("label");
    const dynamicLabel = labels.find((label) => label.text() === "Dynamic Key");
    expect(dynamicLabel).toBeDefined();
  });

  it("updates the value to null if null is provided as dynamic parameters", async () => {
    const initialData = createDynamicInputInitialData({
      initialValue: { existingKey: "existingValue" },
    });
    const updateResult = {
      scope: "#/properties/model/properties/myDynamicSettings",
      providedOptionName: "dynamicSettings",
      values: [
        {
          indices: [],
          value: null,
        },
      ],
    };

    mockInitialUpdate(initialData, updateResult);
    const wrapper = await mountNodeDialog(initialData);

    await flushPromises();

    expect(wrapper.vm.getCurrentData().model.myDynamicSettings).toBeNull();
  });
});
