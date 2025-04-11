import { beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { JsonDataService } from "@knime/ui-extension-service";

import type { InitialData } from "@/nodeDialog/types/InitialData";
import NodeDialog from "../../NodeDialog.vue";
import FlowVariableButton from "../../uiComponents/flowVariables/components/FlowVariableButton.vue";
import { getOptions } from "../utils";

import { mockRegisterSettings } from "./utils/dirtySettingState";
import { dynamicImportsSettled } from "./utils/dynamicImportsSettled";

describe("deprecated flow variables", () => {
  let wrapper: VueWrapper, flowVarButton: VueWrapper<any>;

  const mountNodeDialog = async () => {
    wrapper = mount(NodeDialog as any, getOptions());
    await dynamicImportsSettled(wrapper);
  };

  const expandFlowVariablesPopover = async () => {
    flowVarButton = wrapper.findComponent(FlowVariableButton);
    await flowVarButton.find("button").trigger("mouseup");
    await flushPromises();
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    const uiSchemaKey = "ui_schema";
    const initialDataMock: InitialData = {
      data: { model: { value: "initialValue" } },
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              value: { type: "string" },
            },
          },
        },
      },
      [uiSchemaKey]: {
        // @ts-ignore
        elements: [
          {
            scope: "#/properties/model/properties/value",
            type: "Control",
          },
        ],
      },
      persist: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              value: {
                deprecatedConfigKeys: [
                  {
                    deprecated: [["iAmDeprecated"]],
                  },
                ],
              },
            },
          },
        },
      },
      flowVariableSettings: {
        "model.iAmDeprecated.firstDeprecated": {
          controllingFlowVariableName: "flowVar1",
          exposedFlowVariableName: null,
          controllingFlowVariableAvailable: true,
        },
        "model.iAmDeprecated.secondDeprecated": {
          controllingFlowVariableName: null,
          exposedFlowVariableName: "exposedFlowVar2",
          controllingFlowVariableAvailable: true,
        },
        "model.iAmDeprecated.thirdDeprecated": {
          controllingFlowVariableName: null,
          exposedFlowVariableName: null,
          controllingFlowVariableAvailable: false,
        },
      },
    };
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue(
      initialDataMock,
    );
    mockRegisterSettings();
    await mountNodeDialog();
    await expandFlowVariablesPopover();
  });

  window.ResizeObserver = vi.fn(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
  }));

  it("displays deprecated flow variables", () => {
    expect(flowVarButton.text()).toContain(
      "model.iAmDeprecated.firstDeprecated",
    );
    expect(flowVarButton.text()).toContain(
      "model.iAmDeprecated.secondDeprecated",
    );
    expect(flowVarButton.text()).not.toContain(
      "model.iAmDeprecated.thirdDeprecated",
    );
  });
});
