<script setup lang="ts">
import { type PropType } from "vue";

import AiButton from "./ai-assistant/AiButton.vue";
import type { PaneSizes } from "./utils/paneSizes";

const props = defineProps({
  currentPaneSizes: {
    type: Object as PropType<PaneSizes>,
    required: true,
  },
  showButtonText: {
    type: Boolean,
    default: true,
  },
});
</script>

<template>
  <div class="controls">
    <AiButton
      :current-pane-sizes="props.currentPaneSizes"
      :show-button-text="showButtonText"
    />
    <div class="button-controls">
      <slot name="controls" />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.hide-button-text {
  & :deep(svg) {
    margin-right: 0;
  }
}

.controls {
  display: flex;
  flex-wrap: wrap;
  place-content: center space-between;
  align-items: center;
  width: 100%;
  height: var(--kds-dimension-component-height-2-25x);
  padding-right: var(--kds-spacing-container-1x);
  padding-left: var(--kds-spacing-container-1x);
  background-color: var(--kds-color-surface-default);
  border-top: var(--kds-border-base-subtle);
}

.button-controls {
  display: flex;
  justify-content: right;
  flex-wrap: wrap;
  row-gap: var(--space-4);
}

.button-active {
  &.compact {
    color: var(--knime-white);
    background-color: var(--knime-masala);

    & svg {
      stroke: var(--knime-white);
    }
  }
}
</style>
