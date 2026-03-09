<script setup lang="ts">
import { type PropType, useAttrs } from "vue";
import type { ControlElement } from "@jsonforms/core";
import { rendererProps } from "@jsonforms/vue";

import useVennDiagramBooleanControl, {
  useConstantVennDiagramPart,
} from "./useVennDiagramBooleanControl";

const props = defineProps({
  ...rendererProps<ControlElement>(),
  shape: {
    type: String as PropType<"circle" | "path">,
    required: true,
  },
});

const attrs = useAttrs();

const { isSelected, onClick, disabled } = props.uischema
  ? useVennDiagramBooleanControl(props)
  : useConstantVennDiagramPart(true);
</script>

<template>
  <component
    :is="shape"
    v-bind="attrs"
    :class="[
      'clickable',
      { disabled, selected: isSelected, unselected: !isSelected },
    ]"
    @click="onClick"
  />
</template>

<style scoped lang="postcss">
.clickable {
  cursor: pointer;
  stroke: var(--kds-color-border-neutral);
  stroke-width: var(--kds-core-border-width-xs);

  /* Selected states */
  &.selected {
    fill: var(--kds-color-background-selected-bold-initial);

    &:hover:not(.disabled) {
      fill: var(--kds-color-background-selected-bold-hover);
    }

    &:active:not(.disabled) {
      fill: var(--kds-color-background-selected-bold-active);
    }
  }

  /* Unselected states */
  &.unselected {
    fill: var(--kds-color-background-neutral-bold-initial);

    &:hover:not(.disabled) {
      fill: var(--kds-color-background-neutral-bold-hover);
    }

    &:active:not(.disabled) {
      fill: var(--kds-color-background-neutral-bold-active);
    }
  }

  &.disabled {
    cursor: auto;
  }
}
</style>
