import {
  type Component,
  type ExtractPropTypes,
  type MaybeRef,
  type VNode,
  defineComponent,
  h,
  unref,
} from "vue";
import { rendererProps } from "@jsonforms/vue";

import {
  type VueControl,
  type VueLayout,
  defineControl,
  defineLayout,
} from "@knime/jsonforms";

import FadeIn from "./FadeIn.vue";

const getVNodeFromIsAdvanced = (
  isAdvanced: boolean,
  showAdvancedSettings: boolean,
  renderComponent: () => VNode,
) => {
  if (isAdvanced) {
    if (showAdvancedSettings) {
      return h(FadeIn, renderComponent());
    } else {
      return null;
    }
  } else {
    return renderComponent();
  }
};

export const handleIsAdvanced =
  (showAdvancedSettings: MaybeRef<boolean>) =>
  <D>(component: VueControl<D>): VueControl<D> =>
    defineControl(
      (props, ctx) => () =>
        getVNodeFromIsAdvanced(
          props.control.uischema.options?.isAdvanced,
          unref(showAdvancedSettings),
          () => h(component, props, ctx.slots),
        ),
    );

export const handleIsAdvancedLayout =
  (showAdvancedSettings: MaybeRef<boolean>) =>
  (component: VueLayout): VueLayout =>
    defineLayout(
      (props, ctx) => () =>
        getVNodeFromIsAdvanced(
          props.layout.uischema.options?.isAdvanced,
          unref(showAdvancedSettings),
          () => h(component, props, ctx.slots),
        ),
    );

type RendererProps = ExtractPropTypes<ReturnType<typeof rendererProps>>;

export const handleIsAdvancedRenderer =
  (showAdvancedSettings: MaybeRef<boolean>) =>
  (component: Component<RendererProps>): Component<RendererProps> =>
    defineComponent(
      (props) => () =>
        getVNodeFromIsAdvanced(
          props.uischema.options?.isAdvanced,
          unref(showAdvancedSettings),
          () => h(component, props),
        ),
      {
        props: rendererProps(),
      },
    );
