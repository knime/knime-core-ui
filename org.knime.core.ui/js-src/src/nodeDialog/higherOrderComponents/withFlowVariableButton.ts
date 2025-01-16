import { computed, h } from "vue";

import {
  type ExtractData,
  type VueControl,
  type VueControlProps,
  type VueControlRenderer,
  defineControl,
  mapControls,
} from "@knime/jsonforms";

import { useFlowSettings } from "../composables/components/useFlowVariables";
import type { allControls } from "../renderers";
import FlowVariableButtonWrapper from "../uiComponents/flowVariables/components/FlowVariableButtonWrapper.vue";

type OnControllingHandler<D> = (
  props: VueControlProps<D>,
) => (path: string, value: any, flowVariableName: string) => void;

export const withFlowVariableButton = <D>(
  component: VueControl<D>,
  onControllingFlowVariableSet?: OnControllingHandler<D>,
): VueControl<D> =>
  defineControl((props, ctx) => {
    const path = computed(() => props.control.path);
    const { configPaths, flowSettings, disabledByFlowVariables } =
      useFlowSettings({
        path,
      });
    return () =>
      h(
        component,
        { ...props, disabled: props.disabled || disabledByFlowVariables.value },
        {
          ...ctx.slots,
          buttons: (slotProps: {
            controlHTMLElement: HTMLElement;
            hover: boolean;
          }) => {
            return [
              ...(ctx.slots.buttons ? [ctx.slots.buttons(slotProps)] : []),
              h(FlowVariableButtonWrapper, {
                hover: slotProps.hover,
                dataPath: path.value,
                configPaths,
                flowSettings,
                onControllingFlowVariableSet:
                  onControllingFlowVariableSet?.(props) ||
                  ((path, value) => props.handleChange(path, value as any)),
              }),
            ];
          },
        },
      );
  });

type OnControllingHandlers<T extends Record<string, VueControlRenderer>> =
  Partial<{
    [K in keyof T]: OnControllingHandler<ExtractData<T[K]>>;
  }>;

const onControllingHandlers: OnControllingHandlers<typeof allControls> = {
  credentialsRenderer:
    ({ changeValue }) =>
    (_path, value, flowVariableName) => {
      changeValue({
        ...value,
        flowVariableName,
      });
    },
  legacyCredentialsRenderer:
    ({ handleChange }) =>
    (path, value, flowVariableName) => {
      handleChange(path, {
        ...value,
        flowVariableName,
      });
    },
};

export const mapWithFlowVariables = mapControls((c, k) =>
  withFlowVariableButton(
    c,
    onControllingHandlers[k as keyof typeof onControllingHandlers],
  ),
);
