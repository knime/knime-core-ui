import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { VueWrapper, mount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { InputField } from "@knime/components";
import { JsonDataService } from "@knime/ui-extension-service";

import type { NodeDialogInitialData } from "@/nodeDialog/types/InitialData";
import NodeDialog from "../../../../NodeDialog.vue";
import { mockRegisterSettings } from "../../../../__tests__/__integrationTests__/utils/dirtySettingState";
import { dynamicImportsSettled } from "../../../../__tests__/__integrationTests__/utils/dynamicImportsSettled";
import { getOptions } from "../../../../__tests__/utils";

describe("deprecated flow variables", () => {
  let wrapper: VueWrapper;

  const mountNodeDialog = async () => {
    wrapper = mount(NodeDialog as any, getOptions());
    await dynamicImportsSettled(wrapper);
  };
  const flowVarUserName = "flowVarUsername";

  beforeEach(async () => {
    vi.clearAllMocks();

    const uiSchemaKey = "ui_schema";
    const flowVarName = "myCredentialsVar";
    const initialDataMock: NodeDialogInitialData = {
      data: {
        model: { legacyCredentials: { flowVarName } },
      },
      schema: {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              legacyCredentials: {
                type: "object",
                properties: {
                  credentials: { type: "object" },
                  flowVarName: { type: "string" },
                },
              },
            },
          },
        },
      },
      [uiSchemaKey]: {
        // @ts-ignore
        elements: [
          {
            scope: "#/properties/model/properties/legacyCredentials",
            type: "Control",
            options: {
              format: "legacyCredentials",
            },
          },
        ],
      },
      flowVariableSettings: {},
    };
    vi.spyOn(JsonDataService.prototype, "data").mockImplementation((params) => {
      if (params?.method === "flowVariables.getFlowVariableOverrideValue") {
        return Promise.resolve({
          username: flowVarUserName,
          isHiddenPassword: true,
        });
      }
      return Promise.resolve();
    });
    vi.spyOn(JsonDataService.prototype, "initialData").mockResolvedValue(
      initialDataMock,
    );
    mockRegisterSettings();
    await mountNodeDialog();
    await flushPromises();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("sets flowVarName data as flow variable initially", () => {
    expect(wrapper.findComponent(InputField).props("modelValue")).toBe(
      flowVarUserName,
    );
  });
});
