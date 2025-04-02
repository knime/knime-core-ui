import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { InputField, Label } from "@knime/components";
import { TextControl } from "@knime/jsonforms";
import { JsonDataService } from "@knime/ui-extension-service";

import type { InitialData } from "@/nodeDialog/types/InitialData";
import type { UpdateResult } from "@/nodeDialog/types/Update";
import NodeDialog from "../../../NodeDialog.vue";
import { mockRegisterSettings } from "../../../__tests__/__integrationTests__/utils/dirtySettingState";
import { dynamicImportsSettled } from "../../../__tests__/__integrationTests__/utils/dynamicImportsSettled";
import { getOptions } from "../../../__tests__/utils";

describe("dynamic input", () => {
  let initialDataMock: InitialData;

  beforeEach(() => {
    vi.clearAllMocks();

    const uiSchemaKey = "ui_schema";
    const initialMap = {
      key: "value",
    };
    initialDataMock = {
      data: {
        model: { myDynamicSettings: initialMap },
      },
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              myDynamicSettings: {
                type: "object",
              },
            },
          },
        },
      },
      [uiSchemaKey]: {
        // @ts-ignore
        elements: [
          {
            scope: "#/properties/model/properties/myDynamicSettings",
            type: "Control",
            options: {
              format: "dynamicInput",
            },
            providedOptions: ["dynamicSettings"],
          },
        ],
      },
      flowVariableSettings: {},
    };
  });

  const mountDialog = async (initialDataMock: object) => {
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue(
      initialDataMock,
    );
    mockRegisterSettings();
    const wrapper = mount(NodeDialog as any, getOptions());
    await dynamicImportsSettled(wrapper);
    return wrapper;
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows nothing initially", async () => {
    const wrapper = await mountDialog(initialDataMock);
    expect(wrapper.findComponent(InputField).exists()).toBe(false);
  });

  const mockUpdateResult = ({
    changedValue,
  }: {
    changedValue: Record<string, unknown> | null;
  }) => {
    initialDataMock.globalUpdates = [
      {
        dependencies: [],
        trigger: { id: "afterOpenDialog" },
        triggerInitially: true,
      },
    ];

    // to prevent dynamic import problems, we render an extra text input initially
    // @ts-expect-error
    initialDataMock.schema.properties.model.properties.someTextInput = {
      type: "string",
    };
    // @ts-expect-error
    initialDataMock.ui_schema.elements.push({
      scope: "#/properties/model/properties/someTextInput",
      type: "Control",
    });

    vi.spyOn(JsonDataService.prototype, "data").mockImplementation((params) => {
      if (params?.method === "settings.update2") {
        return Promise.resolve({
          state: "SUCCESS",
          result: [
            {
              scope: "#/properties/model/properties/myDynamicSettings",
              providedOptionName: "dynamicSettings",
              values: [
                {
                  indices: [],
                  value: {
                    data: changedValue,
                    schema: JSON.stringify({
                      type: "object",
                      properties: {
                        key: { type: "string", title: "My title" },
                      },
                    }),
                    uiSchema: JSON.stringify({
                      elements: [
                        {
                          scope: "#/properties/key",
                          type: "Control",
                        },
                      ],
                    }),
                  },
                },
              ],
            },
          ] satisfies UpdateResult[],
        });
      }
      return Promise.resolve();
    });
  };

  const getDynamicallyRenderedInputField = (wrapper: VueWrapper) => {
    const inputFields = wrapper.findAllComponents(TextControl);
    expect(inputFields.length).toBe(2);
    return inputFields[0];
  };

  const getLabelFor = (inputField: VueWrapper, wrapper: VueWrapper) => {
    const inputFieldLabelForId = inputField.attributes("id");
    const label = wrapper
      .findAllComponents(Label)
      .find((l) => l?.attributes("for") === inputFieldLabelForId);
    expect(label).toBeDefined();
    return label!;
  };

  it("shows updated dynamic dialog on an initial update", async () => {
    mockUpdateResult({
      changedValue: {
        key: "otherValue",
      },
    });

    const wrapper = await mountDialog(initialDataMock);
    await flushPromises();

    const inputField = getDynamicallyRenderedInputField(wrapper);
    expect(getLabelFor(inputField, wrapper).text()).toBe("My title");
    expect(inputField.html()).toContain("otherValue");
  });

  it("does not update the value in case null is provided", async () => {
    mockUpdateResult({
      changedValue: null,
    });

    const wrapper = await mountDialog(initialDataMock);
    await flushPromises();

    const inputField = getDynamicallyRenderedInputField(wrapper);
    expect(inputField.html()).toContain("value");
  });
});
