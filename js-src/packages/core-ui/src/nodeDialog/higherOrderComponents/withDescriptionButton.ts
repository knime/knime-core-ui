import { computed, h } from "vue";

import {
  type VueControl,
  type VueLayout,
  defineControl,
  defineLayout,
} from "@knime/jsonforms";

import DescriptionPopover from "../uiComponents/description/DescriptionPopover.vue";

export const withDescriptionButton = <D>(
  component: VueControl<D>,
): VueControl<D> =>
  defineControl((props, ctx) => {
    const description = computed(() => props.control.description);
    return () =>
      h(component, props, {
        ...ctx.slots,
        buttons: (slotProps: {
          controlHTMLElement?: HTMLElement;
          hover: boolean;
        }) => {
          const hasDescription =
            typeof description.value !== "undefined" &&
            description.value !== "";
          return [
            ...(ctx.slots.buttons ? [ctx.slots.buttons(slotProps)] : []),
            ...(hasDescription
              ? [
                  h(DescriptionPopover, {
                    html: description.value,
                    hover: slotProps.hover,
                    ignoredClickOutsideTarget: slotProps.controlHTMLElement,
                  }),
                ]
              : []),
          ];
        },
      });
  });

export const withDescriptionButtonLayout = (component: VueLayout): VueLayout =>
  defineLayout((props, ctx) => {
    const description = computed(() => props.layout.uischema.description);
    return () =>
      h(component, props, {
        ...ctx.slots,
        buttons: (slotProps: { hover: boolean }) => {
          const hasDescription =
            typeof description.value !== "undefined" &&
            description.value !== "";
          return [
            ...(ctx.slots.buttons ? [ctx.slots.buttons(slotProps)] : []),
            ...(hasDescription
              ? [
                  h(DescriptionPopover, {
                    html: description.value,
                    hover: slotProps.hover,
                  }),
                ]
              : []),
          ];
        },
      });
  });
