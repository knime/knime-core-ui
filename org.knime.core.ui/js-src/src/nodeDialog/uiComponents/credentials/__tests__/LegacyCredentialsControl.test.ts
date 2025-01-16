import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { reactive, ref } from "vue";
import type { VueWrapper } from "@vue/test-utils";
import flushPromises from "flush-promises";

import {
  type ProvidedMethods,
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";

import type { FlowSettings } from "@/nodeDialog/api/types";
import { injectionKey as flowVariableSettingsProvidedByControlInjectionKey } from "../../../composables/components/useFlowVariables";
import { injectionKey as flowVariablesMapInjectionKey } from "../../../composables/components/useProvidedFlowVariablesMap";
import { inputFormats } from "../../../constants/inputFormats";
import CredentialsControlBase from "../CredentialsControlBase.vue";
import LegacyCredentialsControl from "../LegacyCredentialsControl.vue";

describe("LegacyCredentialsControl.vue", () => {
  let props: VueControlTestProps<typeof LegacyCredentialsControl>,
    wrapper: VueWrapper;

  const labelForId = "myLabelForId";

  const mountLegacyCredentialsControl = ({
    props,
    provide,
  }: {
    props: VueControlTestProps<typeof LegacyCredentialsControl>;
    provide?: Partial<ProvidedMethods>;
  }) => {
    const flowVariablesMap: Record<string, FlowSettings> = reactive({});
    const setFlowVarState = vi.fn();
    const component = mountJsonFormsControlLabelContent(
      LegacyCredentialsControl,
      {
        props,
        provide: {
          // @ts-expect-error
          flowVariablesApi: {
            getFlowVariableOverrideValue: vi.fn(),
          },
          getPersistSchema: () => ({}),
          [flowVariablesMapInjectionKey as symbol]: flowVariablesMap,
          [flowVariableSettingsProvidedByControlInjectionKey as symbol]: {
            configPaths: ref([
              {
                configPath: "legacyCredentials",
              },
            ]),
            flowSettings: ref({}),
            getSettingStateFlowVariables: vi.fn(() => ({
              controlling: { get: () => ({ set: setFlowVarState }) },
            })),
          },
          ...provide,
        },
      },
    );
    return {
      flowVariablesMap,
      setFlowVarState,
      ...component,
    };
  };

  beforeEach(() => {
    props = {
      control: {
        ...getControlBase("legacyCredentials"),
        data: {
          credentials: {
            username: "username",
            password: "password",
            secondFactor: "secondFactor",
          },
        },
        schema: {
          type: "object",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/legacyCredentials",
          options: {
            format: inputFormats.legacyCredentials,
          },
        },
      },
      labelForId,
      disabled: false,
    };
    const component = mountLegacyCredentialsControl({
      props,
    });
    wrapper = component.wrapper;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("sets legacy flow variable on mounted", async () => {
    const flowVarName = "myFlowVar";
    const flowVarValue = {
      username: "flowVarUsername",
      password: "flowVarPassword",
    };
    const flowVariablesApi = {
      getFlowVariableOverrideValue: vi.fn().mockReturnValue(flowVarValue),
    };
    props.control.data.flowVarName = flowVarName;
    const { flowVariablesMap, changeValue, setFlowVarState } =
      mountLegacyCredentialsControl({
        props,
        provide: {
          // @ts-expect-error
          flowVariablesApi,
        },
      });
    expect(flowVariablesMap[`${props.control.path}`]).toStrictEqual({
      controllingFlowVariableAvailable: true,
      controllingFlowVariableName: flowVarName,
    });
    expect(setFlowVarState).toHaveBeenCalledWith(flowVarName);
    await flushPromises();
    expect(changeValue).toHaveBeenCalledWith({
      credentials: { ...flowVarValue, flowVariableName: flowVarName },
      flowVarName: null,
    });
  });

  it("sets legacy flow variable on mounted which is not available", async () => {
    const flowVarName = "myFlowVar";
    const flowVariablesApi = {
      getFlowVariableOverrideValue: vi.fn(),
    };
    props.control.data.flowVarName = flowVarName;
    const { flowVariablesMap, changeValue, setFlowVarState } =
      mountLegacyCredentialsControl({
        props,
        provide: {
          // @ts-expect-error
          flowVariablesApi,
        },
      });
    expect(flowVariablesMap[`${props.control.path}`]).toStrictEqual({
      controllingFlowVariableAvailable: true,
      controllingFlowVariableName: flowVarName,
    });
    expect(setFlowVarState).toHaveBeenNthCalledWith(1, flowVarName);
    await flushPromises();
    expect(changeValue).toHaveBeenCalledWith({
      credentials: {
        ...props.control.data.credentials,
        flowVariableName: flowVarName,
      },
      flowVarName: null,
    });
    expect(setFlowVarState).toHaveBeenNthCalledWith(2, flowVarName, {
      isFlawed: true,
    });
  });

  it("renders", () => {
    expect(
      wrapper.findComponent(CredentialsControlBase).props().data,
    ).toStrictEqual(props.control.data.credentials);
  });

  it("sets labelForId", () => {
    expect(wrapper.find(".credentials-input-wrapper").attributes().id).toBe(
      labelForId,
    );
  });
});
