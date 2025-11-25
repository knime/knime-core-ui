import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { reactive } from "vue";
import type { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { InputField } from "@knime/components";
import {
  type ProvidedMethods,
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";

import type { FlowSettings } from "@/nodeDialog/api/types";
import { injectionKey as flowVariablesMapInjectionKey } from "../../../composables/components/useProvidedFlowVariablesMap";
import { inputFormats } from "../../../constants/inputFormats";
import CredentialsControl from "../CredentialsControl.vue";

describe("CredentialsControl.vue", () => {
  let props: VueControlTestProps<typeof CredentialsControl>,
    wrapper: VueWrapper,
    changeValue: Mock;
  const labelForId = "myLabelForId";

  const mountCredentialsControl = ({
    props,
    provide,
  }: {
    props: VueControlTestProps<typeof CredentialsControl>;
    provide?: Partial<ProvidedMethods>;
  }) => {
    const flowVariablesMap: Record<string, FlowSettings> = reactive({});
    const component = mountJsonFormsControlLabelContent(CredentialsControl, {
      props,
      provide: {
        ...provide,
        [flowVariablesMapInjectionKey]: flowVariablesMap,
        // @ts-expect-error
        getPersistSchema: () => ({}),
      },
    });
    return {
      flowVariablesMap,
      ...component,
    };
  };

  beforeEach(() => {
    props = {
      control: {
        ...getControlBase("credentials"),
        data: {
          username: "username",
          password: "password",
          secondFactor: "secondFactor",
        },
        schema: {
          properties: {
            credentials: {
              type: "object",
              properties: {
                username: {
                  type: "string",
                },
                password: {
                  type: "string",
                },
                secondFactor: {
                  type: "string",
                },
              },
            },
          },
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/credentials",
          options: {
            format: inputFormats.credentials,
          },
        },
      },
      labelForId,
      disabled: false,
      isValid: true,
    };

    const component = mountCredentialsControl({
      props,
    });
    wrapper = component.wrapper;
    changeValue = component.changeValue;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders", () => {
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers).toHaveLength(2);
    expect(inputFieldWrappers[0].exists()).toBeTruthy();
    expect(inputFieldWrappers[1].exists()).toBeTruthy();
  });

  it("renders with empty data", () => {
    // @ts-expect-error
    props.control.data = {};
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers[0].exists()).toBeTruthy();
    expect(inputFieldWrappers[0].props().modelValue).toBe("");
    expect(inputFieldWrappers[1].exists()).toBeTruthy();
    expect(inputFieldWrappers[1].props().modelValue).toBe("");
  });

  it("sets labelForId", () => {
    expect(wrapper.find(".credentials-input-wrapper").attributes().id).toBe(
      labelForId,
    );
  });

  it("sets correct initial value", () => {
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers[0].vm.modelValue).toBe(
      props.control.data.username,
    );
    expect(inputFieldWrappers[1].vm.modelValue).toBe(
      props.control.data.password,
    );
  });

  it("sets magic password", async () => {
    props.control.data.isHiddenPassword = true;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.findAllComponents(InputField)[1].vm.modelValue).toBe(
      "*****************",
    );
  });

  it("sets correct labels", () => {
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers[0].get("input").attributes().placeholder).toBe(
      "Username",
    );
    expect(inputFieldWrappers[1].get("input").attributes().placeholder).toBe(
      "Password",
    );
  });

  it("updates data when username input is changed", () => {
    const username = "new user";
    wrapper
      .findAllComponents(InputField)[0]
      .vm.$emit("update:modelValue", username);
    expect(changeValue).toHaveBeenCalledWith({
      username,
      password: props.control.data.password,
      secondFactor: props.control.data.secondFactor,
    });
  });

  it("updates data when password input is changed", () => {
    const password = "new password";
    wrapper
      .findAllComponents(InputField)[1]
      .vm.$emit("update:modelValue", password);
    expect(changeValue).toHaveBeenCalledWith({
      username: props.control.data.username,
      password,
      secondFactor: props.control.data.secondFactor,
      isHiddenPassword: false,
    });
  });

  it("clears data if controlling flow variable is unset", async () => {
    const { wrapper, changeValue, flowVariablesMap } = mountCredentialsControl({
      props,
    });

    flowVariablesMap.credentials = {
      controllingFlowVariableName: "some flow variable",
      exposedFlowVariableName: null,
      controllingFlowVariableAvailable: true,
    };
    await flushPromises();
    flowVariablesMap.credentials.controllingFlowVariableName = null;
    await flushPromises();
    await wrapper.setProps({ control: { ...props.control } });
    expect(changeValue).toHaveBeenCalledWith({
      password: "",
      username: "",
      secondFactor: "",
    });
  });

  it("hides username input field when configured to do so", () => {
    props.control.uischema.options!.hasUsername = false;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers).toHaveLength(1);
    expect(inputFieldWrappers[0].get("input").attributes().type).toBe(
      "password",
    );
  });

  it("hides username by state provider", async () => {
    // TODO UIEXT-3073: Remove "as any" once updated to vitest 3.2.4
    const addStateProviderListener = vi.fn() as any;
    props.control.uischema.providedOptions = ["hasUsername"];
    const { wrapper } = mountCredentialsControl({
      props,
      provide: { addStateProviderListener },
    });
    const [{ providedOptionName }, callback] =
      addStateProviderListener.mock.calls[0];
    expect(providedOptionName).toBe("hasUsername");
    expect(wrapper.findAllComponents(InputField)).toHaveLength(2);
    callback(false);
    await flushPromises();
    expect(wrapper.findAllComponents(InputField)).toHaveLength(1);
  });

  it("hides password by state provider", async () => {
    // TODO UIEXT-3073: Remove "as any" once updated to vitest 3.2.4
    const addStateProviderListener = vi.fn() as any;
    props.control.uischema.providedOptions = ["hasUsername"];
    const { wrapper } = mountCredentialsControl({
      props,
      provide: { addStateProviderListener },
    });
    const [{ scope, providedOptionName }, callback] =
      addStateProviderListener.mock.calls[0];
    expect(scope).toBe(props.control.uischema.scope);
    expect(providedOptionName).toBe("hasUsername");
    expect(wrapper.findAllComponents(InputField)).toHaveLength(2);
    callback(false);
    await flushPromises();
    expect(wrapper.findAllComponents(InputField)).toHaveLength(1);
  });

  it("hides password input field when configured to do so", () => {
    props.control.uischema.options!.hasPassword = false;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers).toHaveLength(1);
    expect(inputFieldWrappers[0].get("input").attributes().type).toBe("text");
  });

  it("uses a custom username label if provided with one", () => {
    props.control.uischema.options!.usernameLabel = "Custom Username";
    const { wrapper } = mountCredentialsControl({
      props,
    });
    expect(
      wrapper.findAllComponents(InputField)[0].get("input").attributes()
        .placeholder,
    ).toBe(props.control.uischema.options!.usernameLabel);
  });

  it("uses a custom password label if provided with one", () => {
    props.control.uischema.options!.passwordLabel = "Custom Password";
    const { wrapper } = mountCredentialsControl({
      props,
    });
    expect(
      wrapper.findAllComponents(InputField)[1].get("input").attributes()
        .placeholder,
    ).toBe(props.control.uischema.options!.passwordLabel);
  });

  it("shows second factor input field when configured to do so", () => {
    props.control.uischema.options!.showSecondFactor = true;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers).toHaveLength(3);
    expect(inputFieldWrappers[0].get("input").attributes().type).toBe("text");
    expect(inputFieldWrappers[1].get("input").attributes().type).toBe(
      "password",
    );
    expect(inputFieldWrappers[2].get("input").attributes().type).toBe(
      "password",
    );
  });

  it("does not show second factor input field when password is hidden", () => {
    props.control.uischema.options!.hasPassword = false;
    props.control.uischema.options!.showSecondFactor = true;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers).toHaveLength(1);
    expect(inputFieldWrappers[0].get("input").attributes().type).toBe("text");
  });

  it("updates data when second factor input is changed", () => {
    props.control.uischema.options!.showSecondFactor = true;
    const { wrapper, changeValue } = mountCredentialsControl({ props });
    const secondFactor = "new second factor";
    wrapper
      .findAllComponents(InputField)[2]
      .vm.$emit("update:modelValue", secondFactor);
    expect(changeValue).toHaveBeenCalledWith({
      username: props.control.data.username,
      password: props.control.data.password,
      secondFactor,
      isHiddenSecondFactor: false,
    });
  });

  it("uses a custom second factor label if provided with one", () => {
    props.control.uischema.options!.showSecondFactor = true;
    props.control.uischema.options!.secondFactorLabel = "Custom Second Factor";
    const { wrapper } = mountCredentialsControl({
      props,
    });
    expect(
      wrapper.findAllComponents(InputField)[2].get("input").attributes()
        .placeholder,
    ).toBe(props.control.uischema.options!.secondFactorLabel);
  });

  it("sets magic second factor", async () => {
    props.control.uischema.options!.showSecondFactor = true;
    props.control.data.isHiddenSecondFactor = true;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.findAllComponents(InputField)[2].vm.modelValue).toBe(
      "*****************",
    );
  });

  it("sets correct second factor label", () => {
    props.control.uischema.options!.showSecondFactor = true;
    const { wrapper } = mountCredentialsControl({
      props,
    });
    const inputFieldWrappers = wrapper.findAllComponents(InputField);
    expect(inputFieldWrappers[2].get("input").attributes().placeholder).toBe(
      "Second authentication factor",
    );
  });
});
