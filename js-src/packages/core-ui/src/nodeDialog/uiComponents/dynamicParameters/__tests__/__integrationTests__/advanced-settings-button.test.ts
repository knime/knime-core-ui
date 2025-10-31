import { beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";

import { setupDynamicInputTest } from "./utils";

describe("dynamic parameters - advanced settings button", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows advanced settings button when dynamic parameters with advanced UI schema elements are loaded", async () => {
    const { wrapper } = await setupDynamicInputTest({
      dynamicData: { key: "value", advancedKey: "advancedValue" },
      dynamicSchema: {
        type: "object",
        properties: {
          key: { type: "string" },
          advancedKey: { type: "string" },
        },
      },
      dynamicUiSchema: {
        elements: [
          {
            scope: "#/properties/key",
            type: "Control",
          },
          {
            scope: "#/properties/advancedKey",
            type: "Control",
            options: {
              isAdvanced: true,
            },
          },
        ],
      },
      settingsId: "myDynamicSettings",
    });

    await flushPromises();

    const advancedButton = wrapper.find("a.advanced-options");
    expect(advancedButton.exists()).toBe(true);
    expect(advancedButton.text()).toContain("Show advanced settings");
  });

  it("hides advanced settings button when UI schema without advanced elements is loaded", async () => {
    const { wrapper } = await setupDynamicInputTest({
      dynamicData: { key: "value", otherKey: "otherValue" },
      dynamicSchema: {
        type: "object",
        properties: {
          key: { type: "string" },
          otherKey: { type: "string" },
        },
      },
      dynamicUiSchema: {
        elements: [
          {
            scope: "#/properties/key",
            type: "Control",
          },
          {
            scope: "#/properties/otherKey",
            type: "Control",
          },
        ],
      },
      settingsId: "myDynamicSettings",
    });

    await flushPromises();

    const advancedButton = wrapper.find("a.advanced-options");
    expect(advancedButton.exists()).toBe(false);
  });
});
