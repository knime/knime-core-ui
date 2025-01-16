import { computed, h } from "vue";

import { type VueControl, defineControl } from "@knime/jsonforms";

import StyledReexecutionIcon from "./StyledReexecutionIcon.vue";

export const withReexecutionIcon = <D>(
  component: VueControl<D>,
): VueControl<D> =>
  defineControl((props, ctx) => {
    const isModelSetting = computed(
      () => props.control.uischema.scope?.startsWith("#/properties/model"),
    );
    return () =>
      h(component, props, {
        ...ctx.slots,
        ...(isModelSetting.value && { icon: () => h(StyledReexecutionIcon) }),
      });
  });
