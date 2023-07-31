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
import SimpleTwinlistInput from "../SimpleTwinlistInput.vue";
import LabeledInput from "../LabeledInput.vue";
import Twinlist from "webapps-common/ui/components/forms/Twinlist.vue";

describe("SimpleTwinlistInput.vue", () => {
  let wrapper, onChangeSpy, component;

  beforeAll(() => {
    onChangeSpy = vi.spyOn(SimpleTwinlistInput.methods, "onChange");
    SimpleTwinlistInput.methods.handleChange = vi.fn();
  });

  const path = "test";
  const data = ["test_1"];
  const label = "defaultLabel";

  const getDefaultProps = () => ({
    control: {
      path,
      enabled: true,
      visible: true,
      label,
      data,
      schema: {
        type: "array",
      },
      uischema: {
        options: {
          possibleValues: [
            {
              id: "test_1",
              text: "test_1",
            },
            {
              id: "test_2",
              text: "test_2",
            },
            {
              id: "test_3",
              text: "test_3",
            },
          ],
        },
      },
      rootSchema: {
        hasNodeView: true,
        flowVariablesMap: {},
      },
    },
  });

  beforeEach(() => {
    component = mountJsonFormsComponent(SimpleTwinlistInput, {
      props: getDefaultProps(),
    });
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    expect(wrapper.getComponent(SimpleTwinlistInput).exists()).toBe(true);
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
    expect(wrapper.findComponent(Twinlist).exists()).toBe(true);
  });

  it("initializes jsonforms", () => {
    initializesJsonFormsControl(component);
  });

  it("calls onChange when twinlist input is changed", async () => {
    const dirtySettingsMock = vi.fn();
    const { wrapper } = await mountJsonFormsComponent(SimpleTwinlistInput, {
      props: getDefaultProps(),
      modules: {
        "pagebuilder/dialog": {
          actions: { dirtySettings: dirtySettingsMock },
          namespaced: true,
        },
      },
    });
    await wrapper
      .findComponent(Twinlist)
      .find({ ref: "moveAllRight" })
      .trigger("click");
    expect(onChangeSpy).toBeCalled();
    expect(dirtySettingsMock).not.toHaveBeenCalled();
  });

  it("indicates model settings change when model setting is changed", async () => {
    const dirtySettingsMock = vi.fn();
    const props = getDefaultProps();
    props.control.uischema.scope = "#/properties/model/properties/yAxisColumn";
    const { wrapper } = await mountJsonFormsComponent(SimpleTwinlistInput, {
      props,
      modules: {
        "pagebuilder/dialog": {
          actions: { dirtySettings: dirtySettingsMock },
          namespaced: true,
        },
      },
    });
    await wrapper
      .findComponent(Twinlist)
      .find({ ref: "moveAllRight" })
      .trigger("click");
    expect(onChangeSpy).toBeCalled();
    expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true);
  });

  it("correctly transforms the data into possible values", () => {
    expect(wrapper.findComponent(Twinlist).props().possibleValues).toEqual([
      {
        id: "test_1",
        text: "test_1",
      },
      {
        id: "test_2",
        text: "test_2",
      },
      {
        id: "test_3",
        text: "test_3",
      },
    ]);
  });

  it("sets correct initial value", () => {
    expect(wrapper.findComponent(Twinlist).vm.chosenValues).toStrictEqual(data);
  });

  it("sets correct label", () => {
    expect(wrapper.find("label").text()).toBe(label);
  });

  it("disables twinlist when controlled by a flow variable", () => {
    const props = getDefaultProps();
    props.control.rootSchema.flowVariablesMap[path] = {
      controllingFlowVariableAvailable: true,
      controllingFlowVariableName: "knime.test",
      exposedFlowVariableName: "test",
      leaf: true,
    };
    const { wrapper } = mountJsonFormsComponent(SimpleTwinlistInput, {
      props,
    });
    expect(wrapper.vm.disabled).toBeTruthy();
  });

  it("moves missing values correctly", async () => {
    const dirtySettingsMock = vi.fn();
    const props = getDefaultProps();
    props.control.data = ["missing"];
    const { wrapper } = await mountJsonFormsComponent(SimpleTwinlistInput, {
      props,
      modules: {
        "pagebuilder/dialog": {
          actions: { dirtySettings: dirtySettingsMock },
          namespaced: true,
        },
      },
    });
    expect(wrapper.props().control.data).toStrictEqual(["missing"]);
    await wrapper
      .findComponent(Twinlist)
      .find({ ref: "moveAllLeft" })
      .trigger("click");
    await wrapper.vm.$nextTick();
    expect(onChangeSpy).toBeCalledWith([]);
    await wrapper
      .findComponent(Twinlist)
      .find({ ref: "moveAllRight" })
      .trigger("click");
    expect(onChangeSpy).toBeCalledWith(["test_1", "test_2", "test_3"]);
  });

  it("does not render content of SimpleTwinlistInput when visible is false", async () => {
    wrapper.setProps({
      control: { ...getDefaultProps().control, visible: false },
    });
    await wrapper.vm.$nextTick(); // wait until pending promises are resolved
    expect(wrapper.findComponent(LabeledInput).exists()).toBe(false);
  });
});
