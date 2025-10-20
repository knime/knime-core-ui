import { computed, h } from "vue";

import {
  type VueControl,
  type VueControlProps,
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
                  onControllingFlowVariableSet?.(props) || props.handleChange,
              }),
            ];
          },
        },
      );
  });

const hasNoFlowVariableButton: Partial<Record<keyof typeof allControls, true>> =
  {
    simpleButtonRenderer: true,
    textMessageRenderer: true,
  };

export const mapWithFlowVariables = mapControls((c, k) => {
  if (hasNoFlowVariableButton[k as keyof typeof allControls]) {
    return c;
  }
  return withFlowVariableButton(c);
});
