import { beforeEach, describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import LabeledControl from "../LabeledControl.vue";
import ErrorMessage from "../../ErrorMessage.vue";
import FlowVariableButton from "../../flowVariables/components/FlowVariableButton.vue";
import DescriptionPopover from "../../description/DescriptionPopover.vue";
import ReexecutionIcon from "@knime/styles/img/icons/reexecution.svg";
import { injectionKey as providedByComponentKey } from "@/nodeDialog/composables/components/useFlowVariables";
import { ref } from "vue";
import DialogLabel from "../DialogLabel.vue";
import DialogComponentWrapper from "../../DialogComponentWrapper.vue";
import { Label } from "@knime/components";

describe("LabeledControl.vue", () => {
  let props, flowSettings;

  beforeEach(() => {
    flowSettings = ref(undefined);
    props = {
      control: {
        visible: true,
        path: "path.to.setting",
        schema: {
          configKeys: ["myConfigKey"],
        },
        label: "title",
        rootSchema: {},
        uischema: {},
      },
    };
  });

  const mountLabeledControl = () =>
    mount(LabeledControl, {
      props,
      global: {
        provide: {
          [providedByComponentKey]: { flowSettings },
          getDialogPopoverTeleportDest: () => null,
        },
      },
    });

  it("renders", () => {
    const wrapper = mountLabeledControl();
    expect(wrapper.getComponent(LabeledControl).exists()).toBe(true);
    expect(wrapper.findComponent(ReexecutionIcon).exists()).toBe(false);
    expect(wrapper.findComponent(FlowVariableButton).exists()).toBe(true);
    expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(false);
    expect(wrapper.findComponent(ErrorMessage).exists()).toBe(true);
  });

  it("visually displays model settings", () => {
    props.control.rootSchema.hasNodeView = true;
    props.control.uischema.scope = "#/properties/model/...";
    const wrapper = mountLabeledControl();
    expect(wrapper.findComponent(ReexecutionIcon).exists()).toBe(true);
  });

  it("fills the given space if desired", () => {
    props.fill = true;
    const wrapper = mountLabeledControl();
    expect(
      wrapper
        .findComponent(DialogComponentWrapper)
        .element.classList.contains("fill"),
    ).toBeTruthy();
    expect(
      wrapper.findComponent(DialogLabel).element.classList.contains("fill"),
    ).toBeTruthy();
  });

  it("sets custom margin bottom if desired", () => {
    expect(
      mountLabeledControl().findComponent(DialogComponentWrapper).element.style
        .marginBottom,
    ).toBe("0px");
    props.marginBottom = 30;
    expect(
      mountLabeledControl().findComponent(DialogComponentWrapper).element.style
        .marginBottom,
    ).toBe("30px");
  });

  describe("buttons", () => {
    it("renders the description popover", () => {
      const description = "foo";
      props.control.description = description;
      const wrapper = mountLabeledControl();
      expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(true);
      expect(wrapper.findComponent(DescriptionPopover).props()).toStrictEqual({
        hover: false,
        html: description,
        ignoredClickOutsideTarget: null,
      });
    });

    it("renders the flow variables button", () => {
      const wrapper = mountLabeledControl();
      expect(wrapper.findComponent(FlowVariableButton).exists()).toBe(true);
      expect(wrapper.findComponent(FlowVariableButton).props()).toStrictEqual({
        hover: false,
      });
    });

    it("detects mouse hover", async () => {
      props.control.description = "description";
      const wrapper = mountLabeledControl();
      await wrapper.findComponent(DialogLabel).trigger("mouseover");
      expect(
        wrapper.findComponent(DescriptionPopover).props().hover,
      ).toBeTruthy();
      expect(
        wrapper.findComponent(FlowVariableButton).props().hover,
      ).toBeTruthy();
      await wrapper.findComponent(DialogLabel).trigger("mouseleave");
      expect(
        wrapper.findComponent(DescriptionPopover).props().hover,
      ).toBeFalsy();
      expect(
        wrapper.findComponent(FlowVariableButton).props().hover,
      ).toBeFalsy();
    });
  });

  it("renders error message on error", () => {
    const testError = "test error";
    props.control.errors = testError;
    const wrapper = mountLabeledControl();
    expect(wrapper.findComponent(ErrorMessage).props().errors).toStrictEqual([
      { message: testError },
    ]);
  });

  it("does not render the label line if the label is empty", () => {
    props.control.label = "";
    const wrapper = mountLabeledControl();
    expect(wrapper.findComponent(Label).exists()).toBeFalsy();
  });
});
