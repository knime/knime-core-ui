import { describe, expect, it } from "vitest";
import { ref } from "vue";
import { mount } from "@vue/test-utils";

import { KdsVariableToggleButton } from "@knime/kds-components";

import type { FlowSettings } from "../../../../api/types";
import {
  type ConfigPath,
  injectionKey as flowVariablesInjectionKey,
} from "../../../../composables/components/useFlowVariables";
import type { FlowVariableButtonProps } from "../../types/FlowVariableButtonProps";
import FlowVariableButton from "../FlowVariableButton.vue";
import FlowVariablePopover from "../FlowVariablePopover.vue";

describe("FlowVariableButton.vue", () => {
  const defaultConfigPaths: ConfigPath[] = [
    {
      configPath: "configPath",
      dataPath: "dataPath",
      deprecatedConfigPaths: [],
    },
  ];

  const mountFlowVariableButton = ({
    props = { hover: false },
    configPaths,
    flowSettings = null,
  }: {
    props?: FlowVariableButtonProps;
    configPaths?: ConfigPath[];
    flowSettings?: FlowSettings | null;
  } = {}) => {
    return mount(FlowVariableButton, {
      props,
      global: {
        stubs: {
          FlowVariablePopover: true,
        },
        provide: {
          [flowVariablesInjectionKey as symbol]: {
            configPaths: ref(configPaths ?? defaultConfigPaths),
            flowSettings: ref(flowSettings),
            getSettingStateFlowVariables: () => ({}),
          },
        },
      },
    });
  };

  it("renders KdsVariableToggleButton with correct props", () => {
    const wrapper = mountFlowVariableButton();

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.exists()).toBeTruthy();
    expect(toggleButton.props()).toMatchObject({
      inSet: false,
      outSet: false,
      error: false,
      hidden: true,
    });
  });

  it("does not render when no configPaths are provided", () => {
    const wrapper = mountFlowVariableButton({ configPaths: [] });
    expect(wrapper.findComponent(KdsVariableToggleButton).exists()).toBeFalsy();
  });

  it("passes inSet when controlling flow variable is set", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: "myVar",
        controllingFlowVariableAvailable: true,
        controllingFlowVariableFlawed: false,
        exposedFlowVariableName: null,
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("inSet")).toBe(true);
    expect(toggleButton.props("outSet")).toBe(false);
  });

  it("passes outSet when exposed flow variable is set", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: null,
        controllingFlowVariableAvailable: false,
        controllingFlowVariableFlawed: false,
        exposedFlowVariableName: "myExposedVar",
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("inSet")).toBe(false);
    expect(toggleButton.props("outSet")).toBe(true);
  });

  it("sets hidden to false when hover is true", () => {
    const wrapper = mountFlowVariableButton({ props: { hover: true } });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("hidden")).toBe(false);
  });

  it("sets hidden to false when inSet is true", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: "myVar",
        controllingFlowVariableAvailable: true,
        controllingFlowVariableFlawed: false,
        exposedFlowVariableName: null,
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("hidden")).toBe(false);
  });

  it("sets error to true and hidden to false when variable is missing", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: "myVar",
        controllingFlowVariableAvailable: false,
        exposedFlowVariableName: null,
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("error")).toBe(true);
    expect(toggleButton.props("hidden")).toBe(false);
  });

  it("sets error to true and hidden to false when variable has incorrect type", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: "myVar",
        controllingFlowVariableAvailable: true,
        controllingFlowVariableOfCorrectType: false,
        exposedFlowVariableName: null,
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("error")).toBe(true);
    expect(toggleButton.props("hidden")).toBe(false);
  });

  it("does not set error when variable is available and of correct type", () => {
    const wrapper = mountFlowVariableButton({
      flowSettings: {
        controllingFlowVariableName: "myVar",
        controllingFlowVariableAvailable: true,
        controllingFlowVariableOfCorrectType: true,
        exposedFlowVariableName: null,
      },
    });

    const toggleButton = wrapper.findComponent(KdsVariableToggleButton);
    expect(toggleButton.props("error")).toBe(false);
  });

  it("renders FlowVariablePopover in the slot after clicking the button", async () => {
    const wrapper = mountFlowVariableButton();
    // Click the toggle button to open the popover
    await wrapper.find("button").trigger("click");
    const popover = wrapper.findComponent(FlowVariablePopover);
    expect(popover.exists()).toBeTruthy();
  });

  it("emits controllingFlowVariableSet when FlowVariablePopover emits", async () => {
    const wrapper = mountFlowVariableButton();
    // Click the toggle button to open the popover
    await wrapper.find("button").trigger("click");
    const popover = wrapper.findComponent(FlowVariablePopover);
    popover.vm.$emit(
      "controllingFlowVariableSet",
      "path",
      "value",
      "flowVarName",
    );
    expect(wrapper.emitted("controllingFlowVariableSet")).toStrictEqual([
      ["path", "value", "flowVarName"],
    ]);
  });
});
