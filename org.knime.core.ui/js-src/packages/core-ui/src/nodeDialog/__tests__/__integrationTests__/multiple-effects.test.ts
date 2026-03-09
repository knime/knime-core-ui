import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { JsonFormsDialog, TextControl } from "@knime/jsonforms";
import { JsonDataService } from "@knime/ui-extension-service";

import NodeDialog from "../../NodeDialog.vue";
import { getOptions } from "../utils";

import { mockRegisterSettings } from "./utils/dirtySettingState";
import { dynamicImportsSettled } from "./utils/dynamicImportsSettled";

describe("multiple effects", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRegisterSettings();
  });

  const uiSchemaKey = "ui_schema";

  /**
   * Tests that multiple rules can be applied to a single widget via MultiRuleWrapper:
   * - Outer rule: HIDE the text input when hideToggle is checked
   * - Inner rule: DISABLE the text input when disableToggle is checked
   */
  it("applies DISABLE and HIDE effects from multiple rules on a single widget", async () => {
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue({
      data: {
        model: {
          disableToggle: false,
          hideToggle: false,
          textValue: "hello",
        },
      },
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              disableToggle: { type: "boolean" },
              hideToggle: { type: "boolean" },
              textValue: { type: "string" },
            },
          },
        },
      },
      [uiSchemaKey]: {
        elements: [
          {
            type: "Control",
            scope: "#/properties/model/properties/disableToggle",
          },
          {
            type: "Control",
            scope: "#/properties/model/properties/hideToggle",
          },
          {
            type: "MultiRuleWrapper",
            rule: {
              effect: "HIDE",
              condition: {
                scope: "#/properties/model/properties/hideToggle",
                schema: { const: true },
              },
            },
            elements: [
              {
                type: "Control",
                scope: "#/properties/model/properties/textValue",
                rule: {
                  effect: "DISABLE",
                  condition: {
                    scope: "#/properties/model/properties/disableToggle",
                    schema: { const: true },
                  },
                },
              },
            ],
          },
        ],
      },
      globalUpdates: [],
      initialUpdates: [],
      flowVariableSettings: {},
    });

    const wrapper = mount(NodeDialog as any, getOptions());
    await dynamicImportsSettled(wrapper);
    // Additional settling for async renderers (MultiRuleWrapper, TextControl)
    await flushPromises();
    await vi.dynamicImportSettled();
    await flushPromises();

    const getTextControl = () => wrapper.findComponent(TextControl as any);

    // Initially: textValue is visible and enabled
    expect(getTextControl().exists()).toBe(true);
    expect(getTextControl().find("input").element.disabled).toBe(false);

    const data = wrapper.findComponent(JsonFormsDialog).props("data") as {
      model: {
        disableToggle: boolean;
        hideToggle: boolean;
        textValue: string;
      };
    };

    // Check disableToggle → textValue should be disabled but still visible
    data.model.disableToggle = true;
    await nextTick();
    await flushPromises();

    expect(getTextControl().exists()).toBe(true);
    expect(getTextControl().find("input").element.disabled).toBe(true);

    // Check hideToggle → textValue should be hidden (MultiRuleWrapper outer rule fires)
    data.model.hideToggle = true;
    await nextTick();
    await flushPromises();

    expect(getTextControl().exists()).toBe(false);
  });
});
