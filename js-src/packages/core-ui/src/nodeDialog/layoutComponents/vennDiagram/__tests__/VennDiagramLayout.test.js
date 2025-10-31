import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { mount } from "@vue/test-utils";
import { DispatchRenderer } from "@jsonforms/vue";
import * as jsonformsVueModule from "@jsonforms/vue";

import { getControlBase } from "@knime/jsonforms/testing";

import { injectionKey as flowVaiablesMapInjectionKey } from "../../../composables/components/useProvidedFlowVariablesMap";
import Inner from "../Inner.vue";
import Left from "../Left.vue";
import Right from "../Right.vue";
import VennDiagram from "../VennDiagram.vue";
import VennDiagramLayout from "../VennDiagramLayout.vue";

describe("VennDiagramLayout.vue", () => {
  let wrapper, handleChange;
  const uischema = {
    type: "Control",
    scope: "#/properties/view/properties/xAxisLabel",
    elements: [
      {
        type: "Control",
        scope: "#/properties/middle",
      },
      {
        type: "Control",
        scope: "#/properties/left",
      },
      {
        type: "Control",
        scope: "#/properties/right",
      },
    ],
  };

  beforeEach(() => {
    const control = ref({
      ...getControlBase("test"),
      schema: {
        properties: {
          xAxisLabel: {
            type: "string",
            title: "X Axis Label",
          },
        },
        default: "default value",
      },
      uischema,
    });

    handleChange = vi.fn();

    vi.spyOn(jsonformsVueModule, "useJsonFormsControl").mockReturnValue({
      handleChange,
      control,
    });

    wrapper = mount(VennDiagramLayout, {
      props: {
        uischema,
      },
      global: {
        provide: {
          getPersistSchema: () => ({}),
          [flowVaiablesMapInjectionKey]: {},
        },
        stubs: {
          DispatchRenderer: true,
        },
      },
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(VennDiagramLayout).exists()).toBe(true);
    expect(wrapper.getComponent(VennDiagram).exists()).toBe(true);
    expect(
      wrapper
        .findAllComponents(DispatchRenderer)
        .map((wrapper) => wrapper.props().uischema),
    ).toStrictEqual(uischema.elements);
  });

  it.each([Left, Right, Inner])(
    "triggers change on click on svg part",
    (comp) => {
      wrapper.findComponent(comp).trigger("click");
      expect(handleChange).toHaveBeenCalled();
    },
  );
});
