<script>
import SwitchIcon from 'webapps-common/ui/assets/img/icons/arrow-prev.svg';

export default {
    components: {
        SwitchIcon,
    },
    props: {
        expanded: {
            type: Boolean,
            default: false,
        },
        disabled: {
            type: Boolean,
            default: false,
        },
        /**
         *  Expanded width of the panel's content.
         *  Should be a fixed width.
         */
        width: {
            type: String,
            default: '250px',
            validator: (str) => /^\d+\w+$/.test(str),
        },
        /**
         * The hover title to be shown when the panel is collapsed
         */
        title: {
            type: String,
            default: null,
        },
    },
    emits: ['toggle-expand'],
    data: () => ({
        showContainerTransition: false,
    }),
    mounted() {
        this.showContainerTransition = true;

        requestAnimationFrame(() => {
            this.showContainerTransition = false;
        });
    },
};
</script>

<template>
  <div class="panel">
    <div
      :class="['container',{ 'no-transition': showContainerTransition}]"
      :style="{ width: expanded ? width : 0 }"
    >
      <div
        class="hidden-content"
        :style="{ width }"
      >
        <slot />
      </div>
    </div>

    <button
      :title="expanded ? null : title"
      :disabled="disabled"
      @click="$emit('toggle-expand')"
    >
      <SwitchIcon :style="{ transform: expanded ? null : 'scaleX(-1)' }" />
    </button>
  </div>
</template>

<style lang="postcss" scoped>

.panel {
  display: flex;
  flex: 1 0;
}

.no-transition {
  transition: none !important;
}

.container {
  background-color: inherit;
  overflow-x: hidden;
  transition: width 0.3s ease;
}

button {
  border: none;
  width: 10px;
  padding: 0;
  background-color: var(--knime-silver-sand-semi);
  cursor: pointer;

  &:hover {
    background-color: var(--knime-silver-sand-semi);
  }

  & svg {
    stroke: var(--knime-masala);
    transition: transform 0.3s ease;
  }
}
</style>
