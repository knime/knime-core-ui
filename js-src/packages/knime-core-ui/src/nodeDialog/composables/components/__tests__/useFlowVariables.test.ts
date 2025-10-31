import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { mount } from "@vue/test-utils";

import { createPersistSchema } from "@@/test-setup/utils/createPersistSchema";
import type { FlowSettings } from "@/nodeDialog/api/types";
import type { PersistSchema } from "@/nodeDialog/types/Persist";
import { injectionKey as flowVarMapKey } from "../useProvidedFlowVariablesMap";

import UseFlowVariablesTestComponent from "./UseFlowVariablesTestComponent.vue";

let flowVariablesMap: Record<string, FlowSettings>;

vi.mock("../../utils/inject", () => ({
  injectForFlowVariables: () => () => flowVariablesMap,
}));

type Props = {
  path: string;
  persistSchema: PersistSchema;
};

describe("useFlowVariables", () => {
  beforeEach(() => {
    flowVariablesMap = {};
  });

  const createProps = (params: {
    path: string;
    configPaths?: string[][];
    configKey?: string;
    leaf?: PersistSchema;
  }): Props => {
    return { path: params.path, persistSchema: createPersistSchema(params) };
  };

  const mountTestComponent = (props: Props) => {
    return mount(UseFlowVariablesTestComponent, {
      props: {
        path: ref(props.path),
      },
      global: {
        provide: {
          [flowVarMapKey as symbol]: flowVariablesMap,
          getPersistSchema: () => props.persistSchema,
        },
      },
    });
  };

  const singlePathTestComponentConfigPath = "configKey.subConfigKey";
  const singlePathTestComponentDataPath = "path.subConfigKey";

  const mountSinglePathTestComponent = (params?: { isNew: boolean }) => {
    const path = "path";
    const configKey = "configKey";
    const subConfigKey = "subConfigKey";
    const leaf: PersistSchema = {
      type: "object",
      properties: {
        [subConfigKey]: {},
      },
    };
    return mountTestComponent({
      ...createProps({
        path,
        configKey,
        leaf,
      }),
      ...params,
    });
  };

  it("returns flowSettings, configPaths and disabledByFlowVariables", () => {
    const wrapper = mountSinglePathTestComponent();
    expect(wrapper.vm.flowSettings).toBeDefined();
    expect(wrapper.vm.configPaths).toEqual([
      {
        configPath: singlePathTestComponentConfigPath,
        dataPath: singlePathTestComponentDataPath,
        deprecatedConfigPaths: [],
      },
    ]);
    expect(wrapper.vm.disabledByFlowVariables).toBe(false);
  });

  describe("flow settings", () => {
    const getFlowSettings = (props: Props) => {
      return mountTestComponent(props).vm.flowSettings;
    };

    beforeEach(() => {
      flowVariablesMap = {};
    });

    const createFlowSetting = (
      controllingFlowVariableAvailable: boolean,
      controllingFlowVariableName: string | null,
      exposedFlowVariableName: string | null,
    ): FlowSettings => ({
      controllingFlowVariableAvailable,
      controllingFlowVariableName,
      exposedFlowVariableName,
    });

    const CONTROLLING_FLOW_SETTINGS = createFlowSetting(
      true,
      "my_controlling_variable",
      null,
    );
    const EXPOSING_FLOW_SETTINGS = createFlowSetting(
      false,
      null,
      "my_exposed_variable",
    );
    const NOTHING_FLOW_SETTINGS = createFlowSetting(false, null, null);
    const MERGED_FLOW_SETTINGS = createFlowSetting(
      true,
      "my_controlling_variable",
      "my_exposed_variable",
    );

    it("returns null for an missing flowVariablesMap", () => {
      expect(getFlowSettings(createProps({ path: "otherPath" }))).toBeNull();
    });

    it("returns undefined for missing flow setting for path", () => {
      flowVariablesMap = {
        "path.to.my_setting": CONTROLLING_FLOW_SETTINGS,
      };
      expect(
        getFlowSettings(
          createProps({
            path: "path.to.another_setting",
          }),
        ),
      ).toBeNull();

      expect(
        getFlowSettings(
          createProps({
            path: "path.to.another_setting",
            configPaths: [["also_another_setting"]],
          }),
        ),
      ).toBeNull();
    });

    it("uses path if configPaths is undefined", () => {
      const path = "path.to.my_setting";
      flowVariablesMap = { [path]: CONTROLLING_FLOW_SETTINGS };
      expect(getFlowSettings(createProps({ path }))).toEqual(
        CONTROLLING_FLOW_SETTINGS,
      );
    });

    it("uses configPaths", () => {
      flowVariablesMap = {
        "path.to.my_real_setting_name": CONTROLLING_FLOW_SETTINGS,
      };
      expect(
        getFlowSettings(
          createProps({
            path: "path.to.my_setting",
            configPaths: [["my_real_setting_name"]],
          }),
        ),
      ).toEqual(CONTROLLING_FLOW_SETTINGS);
    });

    it("merges flow settings for configPaths", () => {
      flowVariablesMap = {
        "path.to.setting_1": EXPOSING_FLOW_SETTINGS,
        "path.to.setting_2": CONTROLLING_FLOW_SETTINGS,
      };
      expect(
        getFlowSettings(
          createProps({
            path: "path.to.my_setting",
            configPaths: [
              ["setting_1"],
              ["setting_2"],
              ["not_overwritten_setting"],
            ],
          }),
        ),
      ).toEqual(MERGED_FLOW_SETTINGS);

      flowVariablesMap = {
        "path.to.setting_1": NOTHING_FLOW_SETTINGS,
        "path.to.setting_2": CONTROLLING_FLOW_SETTINGS,
        "path.to.setting_3": EXPOSING_FLOW_SETTINGS,
      };
      expect(
        getFlowSettings(
          createProps({
            path: "path.to.my_setting",
            configPaths: [
              ["setting_1"],
              ["setting_2"],
              ["setting_3"],
              ["not_overwritten_setting"],
            ],
          }),
        ),
      ).toEqual(MERGED_FLOW_SETTINGS);
    });

    it("respects flow settings at paths starting with a config path", () => {
      flowVariablesMap = {
        "path.to_2.my_setting": CONTROLLING_FLOW_SETTINGS,
        "path.to_2.my_setting_2": EXPOSING_FLOW_SETTINGS,
      };
      expect(
        getFlowSettings(
          createProps({
            path: "path.to",
            configPaths: [["to_2"]],
          }),
        ),
      ).toEqual(MERGED_FLOW_SETTINGS);
    });
  });
});
