import {
  afterEach,
  beforeEach,
  beforeAll,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import {
  mountJsonFormsComponent,
  initializesJsonFormsControl,
} from "@@/test-setup/utils/jsonFormsTestUtils";
import NumberInput from "../NumberInput.vue";
import NumberInputBase from "../NumberInputBase.vue";
import ErrorMessage from "../ErrorMessage.vue";
import LabeledInput from "../LabeledInput.vue";

describe("NumberInput.vue", () => {
  let defaultProps, wrapper, onChangeSpy, component;

  beforeAll(() => {
    onChangeSpy = vi.spyOn(NumberInputBase.methods, "onChange");
  });

  beforeEach(() => {
    defaultProps = {
      control: {
        path: "test",
        enabled: true,
        visible: true,
        label: "defaultLabel",
        schema: {
          properties: {
            maxRows: {
              type: "double",
              title: "Show tooltip",
            },
          },
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/maxRows",
          options: {
            format: "double",
          },
        },
        rootSchema: {
          hasNodeView: true,
          flowVariablesMap: {},
        },
      },
    };

    component = mountJsonFormsComponent(NumberInput, { props: defaultProps });
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(NumberInput).exists()).toBe(true);
    expect(wrapper.getComponent(NumberInputBase).exists()).toBe(true);
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
    expect(
      wrapper.getComponent(NumberInput).getComponent(ErrorMessage).exists(),
    ).toBe(true);
  });

  it("passes default props", () => {
    const numberInputProps = wrapper.getComponent(NumberInputBase).props();
    expect(numberInputProps.type).toBe("double");
  });

  it("initializes jsonforms on pass-through component", () => {
    initializesJsonFormsControl({
      wrapper: wrapper.getComponent(NumberInputBase),
      useJsonFormsControlSpy: component.useJsonFormsControlSpy,
    });
  });

  it("calls onChange of NumberInputBase when number input is changed", () => {
    const dirtySettingsMock = vi.fn();
    const { wrapper } = mountJsonFormsComponent(NumberInput, {
      props: defaultProps,
      modules: {
        "pagebuilder/dialog": {
          actions: { dirtySettings: dirtySettingsMock },
          namespaced: true,
        },
      },
    });
    wrapper.findComponent(NumberInputBase).find("input").trigger("input");
    expect(onChangeSpy).toBeCalled();
    expect(dirtySettingsMock).not.toHaveBeenCalled();
  });

  it("indicates model settings change when model setting is changed", () => {
    const dirtySettingsMock = vi.fn();
    const { wrapper } = mountJsonFormsComponent(NumberInput, {
      props: {
        ...defaultProps,
        control: {
          ...defaultProps.control,
          uischema: {
            ...defaultProps.control.schema,
            scope: "#/properties/model/properties/yAxisColumn",
          },
        },
      },
      modules: {
        "pagebuilder/dialog": {
          actions: { dirtySettings: dirtySettingsMock },
          namespaced: true,
        },
      },
    });
    wrapper.findComponent(NumberInputBase).find("input").trigger("input");
    expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
  });

  it("sets correct label", () => {
    expect(wrapper.find("label").text()).toBe(defaultProps.control.label);
  });

  it("disables numberInputBase when controlled by a flow variable", () => {
    const localDefaultProps = JSON.parse(JSON.stringify(defaultProps));
    localDefaultProps.control.rootSchema.flowVariablesMap[
      defaultProps.control.path
    ] = {
      controllingFlowVariableAvailable: true,
      controllingFlowVariableName: "knime.test",
      exposedFlowVariableName: "test",
      leaf: true,
    };

    const { wrapper } = mountJsonFormsComponent(NumberInput, {
      props: localDefaultProps,
    });
    expect(wrapper.findComponent(NumberInputBase).vm.disabled).toBeTruthy();
  });

  it("does not render content of NumberInputBase when visible is false", async () => {
    wrapper.setProps({ control: { ...defaultProps.control, visible: false } });
    await wrapper.vm.$nextTick(); // wait until pending promises are resolved
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(false);
  });

  it("checks that it is not rendered if it is an advanced setting", () => {
    defaultProps.control.uischema.options.isAdvanced = true;
    const { wrapper } = mountJsonFormsComponent(NumberInput, {
      props: defaultProps,
    });
    expect(wrapper.getComponent(NumberInputBase).isVisible()).toBe(false);
  });

  it("checks that it is rendered if it is an advanced setting and advanced settings are shown", () => {
    defaultProps.control.rootSchema = { showAdvancedSettings: true };
    defaultProps.control.uischema.options.isAdvanced = true;
    const { wrapper } = mountJsonFormsComponent(NumberInput, {
      props: defaultProps,
    });
    expect(wrapper.getComponent(NumberInputBase).isVisible()).toBe(true);
  });
});
