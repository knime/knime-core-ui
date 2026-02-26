import { computed, ref } from "vue";
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

  const isSelected = computed(() => control.value.data);

  return {
    isSelected,
    onClick,
    disabled,
  };
};

export const useConstantVennDiagramPart = (constantValue: boolean) => {
  const onClick = () => {
    // do nothing
  };
  const disabled = ref(true);
  const isSelected = ref(constantValue);

  return {
    isSelected,
    onClick,
    disabled,
  };
};
