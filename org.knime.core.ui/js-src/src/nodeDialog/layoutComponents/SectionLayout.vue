<script>
import { defineComponent } from "vue";
import {
  useJsonFormsLayout,
  rendererProps,
  DispatchRenderer,
} from "@jsonforms/vue";
import VerticalLayoutBase from "./VerticalLayoutBase.vue";
import LayoutComponentWrapper from "./LayoutComponentWrapper.vue";

const SectionLayout = defineComponent({
  name: "SectionLayout",
  components: {
    DispatchRenderer,
    LayoutComponentWrapper,
    VerticalLayoutBase,
  },
  props: {
    ...rendererProps(),
  },
  setup(props) {
    return useJsonFormsLayout(props);
  },
});
export default SectionLayout;
</script>

<template>
  <LayoutComponentWrapper :layout="layout" class="section-container">
    <div class="section">
      <div class="section-header">
        <h3>{{ layout.uischema.label }}</h3>
      </div>
      <VerticalLayoutBase
        #default="{ element, index }"
        :elements="layout.uischema.elements"
      >
        <DispatchRenderer
          :key="`${layout.path}-${index}`"
          :schema="layout.schema"
          :uischema="element"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </VerticalLayoutBase>
    </div>
  </LayoutComponentWrapper>
</template>

<style lang="postcss" scoped>
.section-container:not(:first-child) {
  margin-top: var(--spacing-32);
}

.section {
  & .section-header {
    position: sticky;
    top: 0;
    padding-top: var(--spacing-16);
    background-color: var(--knime-gray-ultra-light);
    z-index: 1;
    margin: 0 calc(-1 * var(--horizontal-dialog-padding));

    & h3 {
      margin: 0 var(--horizontal-dialog-padding) 0;
      border-bottom: 1px solid var(--knime-silver-sand);
      color: var(--knime-masala);
      font-size: 16px;
      line-height: 20px;
    }
  }
}
</style>
