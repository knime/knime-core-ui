import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { mount } from "@vue/test-utils";
import * as jsonformsVueModule from "@jsonforms/vue";
import flushPromises from "flush-promises";

import { FunctionButton, LoadingIcon } from "@knime/components";
import { getControlBase } from "@knime/jsonforms/testing";
import EditIcon from "@knime/styles/img/icons/pencil.svg";
import ResetIcon from "@knime/styles/img/icons/reset-all.svg";

import EditResetButton from "../EditResetButton.vue";

describe("EditResetButton.vue", () => {
  let props, wrapper, handleChange;

  beforeEach(() => {
    const control = ref({
      ...getControlBase("_edit"),
      data: false,
      schema: {
        properties: {
          xAxisLabel: {
            type: "string",
            title: "X Axis Label",
          },
        },
        default: "default value",
      },
      uischema: {
        type: "Control",
        scope: "#/properties/view/properties/xAxisLabel",
        options: {
          isAdvanced: false,
        },
      },
    });
    handleChange = vi.fn();

    vi.spyOn(jsonformsVueModule, "useJsonFormsControl").mockReturnValue({
      handleChange,
      control,
    });

    wrapper = mount(EditResetButton, {
      props,
      global: {
        provide: {
          trigger: vi.fn(),
        },
      },
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders edit button initially", () => {
    expect(wrapper.findComponent(EditIcon).exists()).toBe(true);
    expect(wrapper.findComponent(LoadingIcon).exists()).toBe(false);
    expect(wrapper.findComponent(ResetIcon).exists()).toBe(false);
  });

  it("calls handleChange when edit button is clicked", () => {
    wrapper.findComponent(FunctionButton).vm.$emit("click");
    expect(handleChange).toHaveBeenCalledWith("_edit", true);
  });

  describe("initially active reset button", () => {
    it("shows loading icon while loading", async () => {
      expect(wrapper.findComponent(LoadingIcon).exists()).toBe(false);
      await wrapper.setProps({ isLoading: true });
      expect(wrapper.findComponent(LoadingIcon).exists()).toBe(true);
    });

    it("shows reset button initially if desired", async () => {
      await wrapper.setProps({ initialIsEdited: true });
      await flushPromises();
      expect(handleChange).toHaveBeenCalledWith("_edit", true);
    });
  });
});
