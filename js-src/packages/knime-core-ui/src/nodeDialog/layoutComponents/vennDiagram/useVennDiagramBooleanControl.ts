import { computed, ref } from "vue";
import type { ControlElement } from "@jsonforms/core";
import { type RendererProps, useJsonFormsControl } from "@jsonforms/vue";

import { useFlowSettings } from "../../composables/components/useFlowVariables";

const yellow = "#FFD800";
const grey = "#D9D9D9";

const valueToColor = (value: boolean): string => {
  return value ? yellow : grey;
};

export default (props: RendererProps<ControlElement>) => {
  const { control, handleChange } = useJsonFormsControl(props);

  const { disabledByFlowVariables } = useFlowSettings({
    path: computed(() => control.value.path),
  });

  const disabled = computed(
    () => !control.value.enabled || disabledByFlowVariables.value,
  );

  const onClick = () =>
    disabled.value || handleChange(control.value.path, !control.value.data);

  /**
   * It is not possible to work with opacity on disabled, because the inner section lies over the circles
   */
  const fillColor = computed(() => valueToColor(control.value.data));
  return {
    fillColor,
    onClick,
    disabled,
  };
};

export const useConstantVennDiagramPart = (constantValue: boolean) => {
  const onClick = () => {
    // do nothing
  };
  const disabled = ref(true);
  const fillColor = ref(valueToColor(constantValue));

  return {
    fillColor,
    onClick,
    disabled,
  };
};
