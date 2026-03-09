import { beforeEach, describe, expect, it, vi } from "vitest";
import { type Ref, ref } from "vue";
import { shallowMount } from "@vue/test-utils";

import { Button } from "@knime/components";

import type { FlowVariablesForSettings } from "@/nodeDialog/composables/nodeDialog/useDirtySettings";
import { type FlowSettings } from "../../../../api/types";
import { injectionKey as providedByComponentKey } from "../../../../composables/components/useFlowVariables";
import { injectionKey as flowVarMapKey } from "../../../../composables/components/useProvidedFlowVariablesMap";
import DeprecatedFlowVariables from "../DeprecatedFlowVariables.vue";
import UnsetDeprecatedFlowVariableButton from "../UnsetDeprecatedFlowVariableButton.vue";

describe("FlowVariablePopover", () => {
  let configPaths: Ref<
      { configPath: string; deprecatedConfigPaths: string[] }[]
    >,
    dataPaths: Ref<string[]>,
    flowVariablesMap: Record<string, FlowSettings>;

  const path = "model.myPath";

  beforeEach(() => {
    flowVariablesMap = {};
    configPaths = ref([{ configPath: path, deprecatedConfigPaths: [] }]);
  });

  const mountDeprecatedFlowVariablesComponent = (
    unsetControlling: () => void,
  ) => {
    return shallowMount(DeprecatedFlowVariables, {
      global: {
        provide: {
          [providedByComponentKey as symbol]: {
            dataPaths,
            configPaths,
            getSettingStateFlowVariables: () =>
              ({
                controlling: {
                  create: () => ({
                    set: () => {},
                    unset: unsetControlling,
                  }),
                  get: () => ({
                    set: () => {},
                    unset: unsetControlling,
                  }),
                },
                exposed: {
                  create: () => ({
                    set: () => {},
                    unset: () => {},
                  }),
                  get: () => ({
                    set: () => {},
                    unset: () => {},
                  }),
                },
              }) satisfies FlowVariablesForSettings,
          },
          [flowVarMapKey as symbol]: flowVariablesMap,
        },
        stubs: {
          UnsetDeprecatedFlowVariableButton,
        },
      },
    });
  };

  it("renders DeprecatedFlowVariables component if deprecated flow variables exist", async () => {
    const deprecatedPath = "model.myDeprecatedPath";
    const flowVariableName = "aFlowVariableSetAgesAgo";
    flowVariablesMap = {
      [deprecatedPath]: {
        controllingFlowVariableName: flowVariableName,
        controllingFlowVariableAvailable: true,
        exposedFlowVariableName: null,
      },
    };
    configPaths.value = [
      {
        configPath: path,
        deprecatedConfigPaths: [deprecatedPath, "some other deprecated path"],
      },
    ];
    const unsetControlling = vi.fn();
    const wrapper = mountDeprecatedFlowVariablesComponent(unsetControlling);
    expect(wrapper.text()).toContain(
      "The following set flow variables are deprecated:",
    );
    const li = wrapper.find("li");
    expect(li.text()).toContain(deprecatedPath);
    expect(li.find("input").element.value).toStrictEqual(flowVariableName);
    await li.findComponent(Button).trigger("click");
    expect(flowVariablesMap).toStrictEqual({});
    expect(unsetControlling).toHaveBeenCalled();
  });
});
