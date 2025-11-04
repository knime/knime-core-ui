<script setup lang="ts">
import { type PropType } from "vue";

import AiButton from "@s/components/ai-assistant/AiButton.vue";
import type { PaneSizes } from "@s/components/utils/paneSizes";

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
  padding: var(--space-4) var(--space-8);
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
  height: var(--controls-height);
  width: 100%;
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
