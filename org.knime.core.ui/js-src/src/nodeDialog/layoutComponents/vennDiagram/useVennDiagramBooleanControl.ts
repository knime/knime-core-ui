import { computed } from "vue";
import type { ControlElement } from "@jsonforms/core";
import { type RendererProps, useJsonFormsControl } from "@jsonforms/vue";

import { useFlowSettings } from "../../composables/components/useFlowVariables";

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

  const yellow = "#FFD800";
  const grey = "#D9D9D9";
  const lightYellow = "#FFEC80";
  const lightGrey = "#ECECEC";

  /**
   * It is not possible to work with opacity on disabled, because the inner section lies over the circles
   */
  const fillColor = computed(() => {
    if (control.value.data) {
      return disabled.value ? lightYellow : yellow;
    }
    return disabled.value ? lightGrey : grey;
  });
  return {
    fillColor,
    onClick,
    disabled,
  };
};
