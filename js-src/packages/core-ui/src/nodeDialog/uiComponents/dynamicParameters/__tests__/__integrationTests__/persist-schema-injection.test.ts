import { beforeEach, describe, expect, it, vi } from "vitest";
import flushPromises from "flush-promises";

import { mockRegisterSettings } from "@/nodeDialog/__tests__/__integrationTests__/utils/dirtySettingState";
import type { PossibleFlowVariable } from "@/nodeDialog/api/types";
import type {
  PersistSchema,
  PersistTreeSchema,
} from "@/nodeDialog/types/Persist";
import FlowVariableButton from "@/nodeDialog/uiComponents/flowVariables/components/FlowVariableButton.vue";

import { setupDynamicInputTest } from "./utils";

describe("dynamic parameters - persist schema injection", () => {
  const possibleFlowVariables: PossibleFlowVariable[] = [
    {
      abbreviated: true,
      name: "flowVar1",
      value: "flow value",
      type: {
        id: "STRING",
        text: "String",
      },
    },
  ];

  let dataServiceCallHistory: any[] = [];

  beforeEach(() => {
    vi.clearAllMocks();
    mockRegisterSettings();
    dataServiceCallHistory = [];

    // Mock ResizeObserver
    window.ResizeObserver = vi.fn(() => ({
      observe: vi.fn(),
      unobserve: vi.fn(),
      disconnect: vi.fn(),
    })) as any;
  });

  it.each([
    ["", null, "model.myDynamicSettings.customKey"],
    [
      " with outer propertiesRoute",
      {
        type: "object",
        properties: {
          model: {
            type: "object",
            properties: {
              myDynamicSettings: {
                type: "object",
                propertiesRoute: [".."],
                properties: {},
              },
            },
          },
        },
      } satisfies PersistTreeSchema,
      "model.customKey",
    ],
  ])(
    "injects persist schema for dynamic settings%s",
    async (_desc, outerPersistSchema, expectedConfigPath) => {
      const persistSchema: PersistSchema = {
        type: "object",
        properties: {
          key: {
            configKey: "customKey",
          },
        },
      };

      const { wrapper } = await setupDynamicInputTest({
        dynamicData: { key: "value" },
        settingsId: "someSettingsId",
        persist: persistSchema,
        outerPersistSchema,
        additionalDataServiceHandlers: (params) => {
          dataServiceCallHistory.push(params);
          if (params?.method === "flowVariables.getAvailableFlowVariables") {
            return Promise.resolve(possibleFlowVariables);
          }
          if (params?.method === "flowVariables.getFlowVariableOverrideValue") {
            return Promise.resolve("overridden value");
          }
          // eslint-disable-next-line no-undefined
          return undefined;
        },
      });

      await flushPromises();

      // Wait for the dynamic update to complete and UI to render
      await flushPromises();
      await wrapper.vm.$nextTick();

      // Find all flow variable buttons after dynamic content is rendered
      const flowVarButtons = wrapper.findAllComponents(FlowVariableButton);

      // Should have at least 2: one for someTextInput, one for the dynamic field
      expect(flowVarButtons.length).toBeGreaterThan(1);

      // The first button should be for the dynamic field
      const dynamicFieldFlowVarButton = flowVarButtons[0];

      // Click to open the flow variable popover
      await dynamicFieldFlowVarButton.find("button").trigger("mouseup");
      await flushPromises();

      const flowVarCalls = dataServiceCallHistory.filter(
        (call) => call?.method === "flowVariables.getAvailableFlowVariables",
      );
      expect(flowVarCalls.length).toBe(1);
      expect(flowVarCalls[0].options[1]).toStrictEqual(
        expectedConfigPath.split("."),
      );
    },
  );
});
