import { beforeEach, describe, expect, it, vi, type SpyInstance } from "vitest";
import { DOMWrapper, mount, VueWrapper } from "@vue/test-utils";
import Dropdown from "webapps-common/ui/components/forms/Dropdown.vue";
import { JsonDataService } from "@knime/ui-extension-service";

import NodeDialog from "../../NodeDialog.vue";
import flushPromises from "flush-promises";

import FlowVariableButton from "@/nodeDialog/uiComponents/flowVariables/FlowVariableButton.vue";
import { getOptions } from "../utils";

import type {
  FlowSettings,
  PossibleFlowVariable,
} from "@/nodeDialog/api/types";

describe("flow variables", () => {
  const flowVar1 = {
    abbreviated: true,
    name: "flowVar1",
    value: "abbreviated value",
  };

  const possibleFlowVariables: Record<string, PossibleFlowVariable[]> = {
    STRING: [
      {
        abbreviated: true,
        name: "flowVar1",
        value: "abbreviated value",
      },
    ],
  };

  const fetchedFlowVariableValue = "fetchedValue";

  type FlowVariablesMap = Record<string, FlowSettings>;
  type Wrapper = VueWrapper<any> & {
    vm: {
      schema: {
        flowVariablesMap: Record<string, any>;
        getData(): any;
      };
    };
  };

  let dataServiceSpy: SpyInstance<any>,
    wrapper: Wrapper,
    flowVariablesMap: FlowVariablesMap,
    flowVarButton: VueWrapper<any>,
    dropdownButton: DOMWrapper<HTMLButtonElement>,
    listItems: DOMWrapper<HTMLLIElement>[];

  const mountNodeDialog = async () => {
    wrapper = mount(NodeDialog as any, getOptions()) as Wrapper;
    await flushPromises();
    flowVariablesMap = wrapper.vm.schema.flowVariablesMap;
  };

  const expandFlowVariablesPopover = async () => {
    flowVarButton = wrapper.findComponent(FlowVariableButton);
    await flowVarButton.find("button").trigger("mouseup");
    await flushPromises();
    const dropdown = flowVarButton.findComponent(Dropdown);
    dropdownButton = dropdown.find("[role=button]");
    listItems = dropdown.findAll("li");
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    const uiSchemaKey = "ui_schema";
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue({
      data: { model: { value: "initialValue" } },
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              value: { type: "string", configKeys: ["customConfigKey"] },
            },
          },
        },
      },
      [uiSchemaKey]: {
        elements: [
          {
            scope: "#/properties/model/properties/value",
            type: "Control",
          },
        ],
      },
      flowVariableSettings: {},
    });
    dataServiceSpy = vi
      .spyOn(JsonDataService.prototype, "data")
      .mockImplementation((params) => {
        if (params?.method === "flowVariables.getAvailableFlowVariables") {
          return Promise.resolve(possibleFlowVariables);
        }
        if (params?.method === "flowVariables.getFlowVariableOverrideValue") {
          return Promise.resolve(fetchedFlowVariableValue);
        }
        return Promise.resolve();
      });
    await mountNodeDialog();
    await expandFlowVariablesPopover();
  });

  it("displays available flow variables", async () => {
    // Data service is called to receive the possible flow variables
    expect(dataServiceSpy).toHaveBeenNthCalledWith(1, {
      method: "flowVariables.getAvailableFlowVariables",
      options: [
        JSON.stringify({
          data: { model: { value: "initialValue" } },
          flowVariableSettings: {},
        }),
        ["model", "customConfigKey"],
      ],
    });

    expect(dropdownButton.text()).toBe("No flow variable selected");
    expect(flowVariablesMap).toStrictEqual({});
    listItems.forEach((li) => expect(li.isVisible()).toBeFalsy());

    await dropdownButton.trigger("click");

    listItems.forEach((li) => expect(li.isVisible()).toBeTruthy());
    expect(listItems.map((li) => li.text())).toStrictEqual(["", flowVar1.name]);
  });

  it("sets controlling flow variables", async () => {
    // Click on "flowVar1"
    listItems.at(1)?.trigger("click");

    // Data service is called to get the value of the flow variable
    expect(dataServiceSpy).toHaveBeenNthCalledWith(2, {
      method: "flowVariables.getFlowVariableOverrideValue",
      options: [
        JSON.stringify({
          data: { model: { value: "initialValue" } },
          flowVariableSettings: flowVariablesMap,
        }),
        ["model", "value"],
      ],
    });
    await flushPromises();

    expect(dropdownButton.text()).toBe(flowVar1.name);
    expect(flowVariablesMap).toStrictEqual({
      "model.customConfigKey": {
        controllingFlowVariableName: flowVar1.name,
        controllingFlowVariableAvailable: true,
      },
    });
    expect(wrapper.vm.getData().data.model.value).toBe(
      fetchedFlowVariableValue,
    );
  });

  it("unsets controlling flow variables", async () => {
    // Click on "flowVar1"
    listItems.at(1)?.trigger("click");
    await flushPromises();
    // Click back on none option
    listItems.at(0)?.trigger("click");

    expect(flowVariablesMap).toStrictEqual({
      "model.customConfigKey": {
        controllingFlowVariableName: null,
        controllingFlowVariableAvailable: false,
      },
    });
    await flushPromises();
    expect(dropdownButton.text()).toBe("No flow variable selected");

    // We keep the last value to keep a valid state
    expect(wrapper.vm.getData().data.model.value).toBe(
      fetchedFlowVariableValue,
    );
  });

  describe("credentials", () => {
    beforeEach(async () => {
      vi.clearAllMocks();

      const uiSchemaKey = "ui_schema";
      vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue({
        data: {
          model: {
            value: {
              username: "myUsername",
              isHiddenPassword: true,
            },
          },
        },
        schema: {
          type: "object",
          properties: {
            model: {
              type: "object",
              properties: {
                value: {
                  type: "object",
                  properties: {
                    username: {
                      type: "string",
                    },
                    password: {
                      type: "string",
                    },
                    isHiddenPassword: {
                      type: "boolean",
                    },
                    flowVariableName: {
                      type: "string",
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
              scope: "#/properties/model/properties/value",
              type: "Control",
              options: {
                format: "credentials",
              },
            },
          ],
        },
        flowVariableSettings: {},
      });
      dataServiceSpy = vi
        .spyOn(JsonDataService.prototype, "data")
        .mockImplementation((params) => {
          if (params?.method === "flowVariables.getAvailableFlowVariables") {
            return Promise.resolve(possibleFlowVariables);
          }
          if (params?.method === "flowVariables.getFlowVariableOverrideValue") {
            return Promise.resolve({
              username: "flowVarUsername",
              isHiddenPassword: true,
            });
          }
          return Promise.resolve();
        });
      await mountNodeDialog();
      await expandFlowVariablesPopover();
    });

    it("sets flow variable name in data when controlling flow variable is set", async () => {
      // Click on "flowVar1"
      listItems.at(1)?.trigger("click");
      await flushPromises();

      expect(wrapper.vm.getData().data.model.value).toStrictEqual({
        flowVariableName: flowVar1.name,
        isHiddenPassword: true,
        username: "flowVarUsername",
      });
    });

    it("unsets flow variable name from data when controlling flow variable is unset", async () => {
      // Click on "flowVar1"
      listItems.at(1)?.trigger("click");
      await flushPromises();
      listItems.at(0)?.trigger("click");
      await flushPromises();

      expect(wrapper.vm.getData().data.model.value).toStrictEqual({
        username: "",
        secondFactor: "",
        password: "",
      });
    });
  });
});
