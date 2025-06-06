import { beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import { cloneDeep } from "lodash-es";

import { FunctionButton } from "@knime/components";
import { TextControl } from "@knime/jsonforms";
import EditIcon from "@knime/styles/img/icons/pencil.svg";
import ResetIcon from "@knime/styles/img/icons/reset-all.svg";
import { JsonDataService } from "@knime/ui-extension-service";

import NodeDialog from "../../../NodeDialog.vue";
import EditResetButton from "../../../layoutComponents/arrayLayout/EditResetButton.vue";
import type {
  IndexIdsValuePairs,
  Update,
  UpdateResult,
} from "../../../types/Update";
import { getOptions } from "../../utils";
import { mockRegisterSettings } from "../utils/dirtySettingState";
import { dynamicImportsSettled } from "../utils/dynamicImportsSettled";

describe("edit/reset button in array layouts", () => {
  type Wrapper = VueWrapper<any> & {
    vm: {
      schema: {
        flowVariablesMap: Record<string, any>;
        getData(): any;
      };
    };
  };

  beforeEach(() => {
    mockRegisterSettings();
  });

  const defaultTextValue = "theDefault";

  const dependencyScope =
    "#/properties/model/properties/values/items/properties/value";

  const uiSchemaKey = "ui_schema";
  const baseInitialDataJson = {
    data: {
      model: {
        values: [{ value: defaultTextValue }, { value: "notTheDefault" }],
      },
    },
    schema: {
      type: "object",
      properties: {
        model: {
          type: "object",
          properties: {
            values: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  value: {
                    type: "string",
                    default: defaultTextValue,
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
          scope: "#/properties/model/properties/values",
          type: "Control",
          options: {
            arrayElementTitle: "Element",
            withEditAndReset: true,
            detail: [
              {
                scope: "#/properties/value",
                type: "Control",
              },
            ],
          },
        } as any,
      ],
    },
    globalUpdates: [
      {
        trigger: {
          id: "ElementResetButton",
        },
        dependencies: [dependencyScope],
      },
    ] as Update[],
    initialUpdates: [] as UpdateResult[],
    flowVariableSettings: {},
  };

  let initialDataJson: typeof baseInitialDataJson;

  beforeEach(() => {
    initialDataJson = cloneDeep(baseInitialDataJson);
  });

  const mockInitialData = () => {
    vi.clearAllMocks();
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue(
      initialDataJson,
    );
  };

  const mountNodeDialog = async () => {
    mockInitialData();
    const wrapper = mount(NodeDialog as any, getOptions()) as Wrapper;
    await dynamicImportsSettled(wrapper); // settle vertical layout and array layout
    await flushPromises();
    return wrapper;
  };

  const mockRPCResult = (
    getResult: (
      dependencies: Record<string, IndexIdsValuePairs>,
    ) => UpdateResult[],
  ) =>
    vi.spyOn(JsonDataService.prototype, "data").mockImplementation((param) => {
      const result = getResult(param!.options![2]);
      return Promise.resolve({
        state: "SUCCESS",
        result,
      });
    });

  const mockRPCResultToResetElementTextValue = () => {
    mockRPCResult((dependencies) => [
      {
        id: null,
        values: dependencies[dependencyScope].map(({ indices }) => ({
          indices,
          value: defaultTextValue,
        })),
        scope: "#/properties/model/properties/values/items/properties/value",
      },
    ]);
  };

  const getNthTextValue = (wrapper: Wrapper, n: number) =>
    wrapper
      .find(".array")
      .findAllComponents(TextControl as any)
      .at(n)
      .find("input").element.value;

  const getNthEditResetButton = (wrapper: Wrapper, n: number) =>
    wrapper
      .find(".array")
      .findAllComponents(EditResetButton as any)
      .at(n);

  it("shows the reset icon when clicking on the edit icon", async () => {
    mockRPCResultToResetElementTextValue();
    const wrapper = await mountNodeDialog();
    getNthEditResetButton(wrapper, 0)
      .findComponent(FunctionButton)
      .vm.$emit("click");
    expect(
      getNthEditResetButton(wrapper, 1).findComponent(ResetIcon).exists(),
    ).toBeTruthy();
  });

  it("resets the value when clicking the reset icon", async () => {
    mockRPCResultToResetElementTextValue();
    const wrapper = await mountNodeDialog();
    getNthEditResetButton(wrapper, 1)
      .findComponent(FunctionButton)
      .vm.$emit("click");
    await flushPromises();
    expect(
      getNthEditResetButton(wrapper, 1).findComponent(EditIcon).exists(),
    ).toBeTruthy();
    expect(getNthTextValue(wrapper, 1)).toBe(defaultTextValue);
  });

  it("shows the appropriate state initially depending on whether a reset changes the value", async () => {
    mockRPCResultToResetElementTextValue();
    const wrapper = await mountNodeDialog();
    expect(
      getNthEditResetButton(wrapper, 0).findComponent(EditIcon).exists(),
    ).toBeTruthy();
    expect(
      getNthEditResetButton(wrapper, 1).findComponent(ResetIcon).exists(),
    ).toBeTruthy();
  });
});
