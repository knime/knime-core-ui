<script>
import { defineComponent } from '@vue/composition-api';
import { useJsonFormsLayout, rendererProps, DispatchRenderer } from '@jsonforms/vue2';
import LayoutComponentWrapper from './LayoutComponentWrapper.vue';
import Carousel from '~/webapps-common/ui/components/Carousel.vue';

const HorizontalLayout = defineComponent({
    name: 'HorizontalLayout',
    components: {
        DispatchRenderer,
        LayoutComponentWrapper,
        Carousel
    },
    props: {
        ...rendererProps()
    },
    setup(props) {
        return useJsonFormsLayout(props);
    },
    computed: {
        flexValues() {
            const numberOfElements = this.layout.uischema.elements.length;
            return this.layout.uischema.options?.ratios?.length === numberOfElements
                ? this.layout.uischema.options.ratios
                : new Array(numberOfElements).fill(1);
        },
        elements() {
            return this.layout.uischema.elements.map(element => (
                { ...element, options: { ...element.options, teleportDescription: true } }));
        }
    }
});
export default HorizontalLayout;
</script>

<template>
  <LayoutComponentWrapper :layout="layout">
    <Carousel>
      <div class="horizontal">
        <div
          v-for="(element, index) in elements"
          :key="`${layout.path}-${index}`"
          :style="{ 'flex': flexValues[index] }"
        >
          <DispatchRenderer
            :schema="layout.schema"
            :uischema="element"
            :path="layout.path"
            :enabled="layout.enabled"
            :renderers="layout.renderers"
            :cells="layout.cells"
          />
        </div>
      </div>
    </Carousel>
  </LayoutComponentWrapper>
</template>

<style lang="postcss" scoped>

.shadow-wrapper {
  --carousel-shadow-distance: 15px; /* distance between the content borders and the shadow of the carousel */

  margin-left: calc(-1 * var(--carousel-shadow-distance));
  margin-right: calc(-1 * var(--carousel-shadow-distance));

  &::before {
    pointer-events: none;
    background-image: linear-gradient(270deg, hsla(0, 0%, 100%, 0) 0%, var(--knime-gray-ultra-light) 100%);
  }

  &::after {
    pointer-events: none;
    background-image: linear-gradient(90deg, hsla(0, 0%, 100%, 0) 0%, var(--knime-gray-ultra-light) 100%);
  }

  & >>> .carousel {
    padding-left: var(--carousel-shadow-distance);
    padding-right: var(--carousel-shadow-distance);
  }
}

.horizontal {
  min-width: 100%;
  display: inline-flex;
  justify-content: space-between;

  & > * {
    margin-left: 10px;
  }

  & > *:first-child {
    margin-left: 0;
  }

  & > *:last-child {
    /* padding-right: 10px; */
  }
}

</style>
