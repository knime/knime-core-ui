import useDialogControl from "@/nodeDialog/composables/components/useDialogControl";
import { ControlElement } from "@jsonforms/core";
import { RendererProps } from "@jsonforms/vue";
import { computed } from "vue";

export default (props: RendererProps<ControlElement>) => {
  const { control, onChange, disabled } = useDialogControl<boolean>({ props });
  const onClick = () => disabled.value || onChange(!control.value.data);

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
