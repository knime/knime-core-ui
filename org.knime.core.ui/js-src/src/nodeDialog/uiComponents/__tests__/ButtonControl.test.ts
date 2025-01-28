import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import type { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { FunctionButton, LoadingIcon } from "@knime/components";
import type { VueControlProps } from "@knime/jsonforms";
import {
  getControlBase,
  mountJsonFormsControl,
} from "@knime/jsonforms/testing";

import ButtonControl from "../ButtonControl.vue";

describe("ButtonControl", () => {
  const states = [
    {
      id: "A",
      text: "Text_A",
      disabled: true,
      primary: true,
      nextState: "B",
    },
    {
      id: "B",
      text: "Text_B",
      disabled: false,
      primary: false,
      nextState: "C",
    },
    {
      id: "C",
      text: "Text_C",
    },
  ];

  const defaultOptions = {
    format: "button",
    states,
    displayErrorMessage: true,
    showTitleAndDescription: true,
    actionHandler: "MyActionHandlerClass",
  };
  const uischema = {
    type: "Control" as const,
    scope: "#/properties/buttonInput",
    options: defaultOptions,
  };
  const schema = {
    properties: {
      buttonInput: {
        type: "string",
        title: "Test title",
      },
    },
  };

  const path = "test";

  const getProps = (uischemaOptions: any) => ({
    control: {
      ...getControlBase(path),
      schema,
      data: undefined,
      label: "Test title",
      uischema: {
        ...uischema,
        options: {
          ...defaultOptions,
          ...uischemaOptions,
        },
      },
    },
    disabled: false,
    isValid: true,
    messages: { errors: [] },
  });

  let getDataResult: any,
    wrapper: VueWrapper<any>,
    props: Omit<
      VueControlProps<any>,
      "handleChange" | "changeValue" | "onRegisterValidation"
    >,
    getData: Mock,
    changeValue: Mock;

  const mountButtonControl = () =>
    mountJsonFormsControl(ButtonControl, {
      props,
      provide: {
        // @ts-expect-error
        getData,
        registerWatcher: vi.fn(({ init }) => {
          init?.({ view: {}, model: {} });
          return () => {};
        }),
      },
    });

  beforeEach(async () => {
    props = getProps(defaultOptions);
    vi.useFakeTimers();
    getDataResult = {
      state: "SUCCESS",
      result: {
        settingValue: "token",
        setSettingValue: true,
        buttonState: states[1].id,
      },
    };
    getData = vi.fn(() => getDataResult);
    const component = await mountButtonControl();
    wrapper = component.wrapper;
    changeValue = component.changeValue;
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  const getButtonComponent = (wrapper: VueWrapper) => {
    return wrapper
      .find(".button-wrapper")
      .findComponent(FunctionButton)
      .find("button");
  };

  describe("renders", () => {
    it("renders main components", () => {
      expect(getButtonComponent(wrapper).exists()).toBeTruthy();
    });

    it("shows loading spinner during loading", async () => {
      wrapper.vm.numPendingRequests = 1;
      await wrapper.vm.$nextTick();
      expect(wrapper.findComponent(LoadingIcon).exists()).toBeTruthy();
    });
  });

  describe("actions", () => {
    it("invokes action on click", async () => {
      const currentSettings = { foo: "bar" };
      wrapper.vm.currentSettings = currentSettings;
      await getButtonComponent(wrapper).trigger("click");
      expect(getData).toHaveBeenCalledWith({
        method: "settings.invokeButtonAction",
        options: [
          expect.anything(),
          uischema.options.actionHandler,
          states[1].id,
          currentSettings,
        ],
      });
    });

    it("sets next state specified in the uischema immediately", () => {
      getButtonComponent(wrapper).trigger("click");
      expect(wrapper.vm.currentState).toStrictEqual(states[2]);
    });

    it("sets next state specified by the returned value", async () => {
      const nextState = states[0];
      getData.mockImplementation(() => ({
        state: "SUCCESS",
        result: { buttonState: nextState.id },
      }));
      await getButtonComponent(wrapper).trigger("click");
      expect(wrapper.vm.currentState).toStrictEqual(nextState);
    });

    it("does not change the state if null is returned successfully", async () => {
      getData.mockImplementation(() => ({
        state: "SUCCESS",
        result: null,
      }));
      getButtonComponent(wrapper).trigger("click");
      const stateAfterClick = wrapper.vm.currentState;
      await wrapper.vm.$nextTick();
      expect(wrapper.vm.currentState).toStrictEqual(stateAfterClick);
    });

    it("calls changeValue if the result should be applied", async () => {
      getData.mockImplementation(() => ({
        state: "SUCCESS",
        result: {
          settingValue: "token",
          setSettingValue: true,
          buttonState: states[1].id,
        },
      }));
      await wrapper
        .findComponent(FunctionButton)
        .find("button")
        .trigger("click");
      vi.runAllTimers();
      expect(changeValue).toHaveBeenCalledWith("token");
    });

    it("does not call changeValue if the result should not be applied", async () => {
      getData.mockImplementation(() => ({
        state: "SUCCESS",
        result: {
          settingValue: "token",
          setSettingValue: false,
          buttonState: states[1].id,
        },
      }));
      vi.runAllTimers();
      changeValue.mockClear();
      await wrapper
        .findComponent(FunctionButton)
        .find("button")
        .trigger("click");
      vi.runAllTimers();
      expect(changeValue).not.toHaveBeenCalled();
    });
  });

  describe("errors", () => {
    const errorReult = {
      state: "FAIL",
      message: ["some error"],
      result: {
        buttonState: states[1].id,
        setSettingValue: false,
        settingValue: null,
      },
    };

    beforeEach(() => {
      getData.mockImplementation(() => errorReult);
    });

    it("displays error message on FAIL", async () => {
      await getButtonComponent(wrapper).trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.find(".button-wrapper").text()).contains(
        "Error: some error",
      );
    });

    it("displays no error message if displayErrorMessage is false", async () => {
      const noErrorUischema = { displayErrorMessage: false };
      props = getProps(noErrorUischema);
      getData = vi.fn(() => errorReult);
      const { wrapper } = await mountButtonControl();
      await wrapper
        .findComponent(FunctionButton)
        .find("button")
        .trigger("click");
      expect(wrapper.find(".button-wrapper").text()).not.contains(
        "Error: some error",
      );
    });

    it("clears error message on next update", async () => {
      await getButtonComponent(wrapper).trigger("click");
      expect(wrapper.find(".button-wrapper").text()).contains(
        "Error: some error",
      );
      getButtonComponent(wrapper).trigger("click");
      await wrapper.vm.$nextTick();
      expect(wrapper.find(".button-wrapper").text()).not.contains(
        "Error: some error",
      );
    });
  });

  describe("dependencies to other settings", () => {
    let settingsChangeCallback: any,
      wrapper: VueWrapper<any>,
      dependencies: string[],
      callbacks: any[];

    const dependenciesUischema = ["foo", "bar"];

    beforeEach(() => {
      props = getProps({ isCancelable: true });
      props.control.uischema.options!.dependencies = dependenciesUischema;
      getData = vi.fn(() => getDataResult);
      callbacks = [];
      const component = mountJsonFormsControl(ButtonControl, {
        props,
        provide: {
          // @ts-expect-error
          getData,
          registerWatcher: vi.fn((callback) => {
            callbacks.push(callback);
            callback.init?.({ view: {}, model: {} });
            return () => {};
          }),
        },
      });
      wrapper = component.wrapper;
      settingsChangeCallback = callbacks[0].transformSettings;
      dependencies = callbacks[0].dependencies;
    });

    it("registers one watcher", () => {
      expect(settingsChangeCallback).toBeDefined();
      expect(dependencies).toStrictEqual(dependenciesUischema);
      expect(callbacks.length).toBe(1);
    });

    it("unpacks new data to current settings", () => {
      settingsChangeCallback({ model: { foo: 2, bar: 1 }, view: { baz: 3 } });
      expect(wrapper.vm.currentSettings).toStrictEqual({
        foo: 2,
        bar: 1,
        baz: 3,
      });
    });
  });

  describe("updates triggered by other settings", () => {
    let settingsChangeCallback: any,
      wrapper: VueWrapper,
      dependencies: string[],
      callbacks: any[],
      changeValue: Mock;

    const dependenciesUpdateHandler = ["foo", "bar"];
    const updateHandler = "updateHandler";

    beforeEach(() => {
      const props = getProps({ isCancelable: true });
      props.control.uischema.options.updateOptions = {
        updateHandler,
        dependencies: dependenciesUpdateHandler,
      };

      callbacks = [];
      const component = mountJsonFormsControl(ButtonControl, {
        props,
        provide: {
          // @ts-expect-error
          getData,
          registerWatcher: vi.fn((callback) => {
            callbacks.push(callback);
            callback.init?.({ view: {}, model: {} });
            return () => {};
          }),
        },
      });

      wrapper = component.wrapper;
      changeValue = component.changeValue;
      settingsChangeCallback = callbacks[1].transformSettings;
      dependencies = callbacks[1].dependencies;
    });

    it("registers one watcher", () => {
      expect(settingsChangeCallback).toBeDefined();
      expect(dependencies).toStrictEqual(dependenciesUpdateHandler);
      expect(callbacks.length).toBe(2);
    });

    it("applies new state defined by the update callback", async () => {
      const settingValue = "updateSettingResult";
      const nextState = states[0];
      getDataResult = {
        state: "SUCCESS",
        result: {
          settingValue,
          setSettingValue: true,
          buttonState: nextState.id,
        },
      };
      await settingsChangeCallback({
        model: { foo: 2, bar: 1 },
        view: { baz: 3 },
      });
      expect(getData).toHaveBeenCalledWith({
        method: "settings.update",
        options: [expect.anything(), updateHandler, { foo: 2, bar: 1, baz: 3 }],
      });
      vi.runAllTimers();
      await flushPromises();
      expect(wrapper.text()).toContain(nextState.text);
      expect(changeValue).toHaveBeenCalledWith(settingValue);
    });
  });

  describe("reset current state", () => {
    it("resets current state after failed request on click", async () => {
      const nextState = states[0];
      getDataResult = {
        state: "FAIL",
        result: {
          buttonState: nextState.id,
        },
      };
      await wrapper
        .findComponent(FunctionButton)
        .find("button")
        .trigger("click");
      expect(wrapper.text()).toContain(states[1].text);
    });

    it("resets current state after canceled request on click", async () => {
      const nextState = states[0];
      getDataResult = {
        state: "CANCELED",
        result: {
          buttonState: nextState.id,
        },
      };
      await wrapper
        .findComponent(FunctionButton)
        .find("button")
        .trigger("click");
      expect(wrapper.text()).toContain(states[1].text);
    });
  });

  it("does not switch to next state on click when none exists", async () => {
    getDataResult.result.buttonState = states[2].id;
    getButtonComponent(wrapper).trigger("click");
    expect(wrapper.text()).toContain(states[1].text);
    await flushPromises();
    expect(wrapper.text()).toContain(states[2].text);
    getButtonComponent(wrapper).trigger("click");
    expect(wrapper.text()).toContain(states[2].text);
  });

  describe("current state", () => {
    const mountWithInitialState = async (initialState: any) => {
      const stateId = "myState";
      const result = {
        state: "SUCCESS",
        result: {
          settingValue: "token",
          setSettingValue: true,
          buttonState: stateId,
        },
      };
      props = getProps({
        states: [
          {
            id: stateId,
            ...initialState,
          },
        ],
      });
      getData = vi.fn(() => result);
      const { wrapper } = mountButtonControl();
      await flushPromises();
      return wrapper;
    };

    it("disabled button if current state is disabled", async () => {
      const wrapper = await mountWithInitialState({ disabled: true });
      expect(getButtonComponent(wrapper).attributes().class).contains(
        "disabled",
      );
    });

    it("does not disabled button if current state is not disabled", async () => {
      const wrapper = await mountWithInitialState({ disabled: false });
      expect(getButtonComponent(wrapper).attributes().class).not.contains(
        "disabled",
      );
    });

    it("sets primary if current state is primary", async () => {
      const wrapper = await mountWithInitialState({ primary: true });
      expect(getButtonComponent(wrapper).attributes().class).contains(
        "primary",
      );
    });

    it("does not set primary if current state is not primary", async () => {
      const wrapper = await mountWithInitialState({ primary: false });
      expect(getButtonComponent(wrapper).attributes().class).not.contains(
        "primary",
      );
    });

    it("displays custom button texts", async () => {
      const text = "customText";
      const wrapper = await mountWithInitialState({ text });
      expect(wrapper.find(".button-input-text").text()).toBe(text);
    });
  });
});
