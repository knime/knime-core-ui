<script setup lang="ts">
import { useId } from "vue";
import type { ControlElement } from "@jsonforms/core";
import { rendererProps } from "@jsonforms/vue";

import Inner from "./Inner.vue";
import Left from "./Left.vue";
import Right from "./Right.vue";

const props = defineProps(
  rendererProps<{
    elements:
      | [left: ControlElement, middle: ControlElement, right: ControlElement]
      | [left: ControlElement, right: ControlElement];
  }>(),
);
const [first, second, third] = props.uischema.elements;
let left: ControlElement,
  middle: ControlElement | undefined,
  right: ControlElement;
if (typeof third === "undefined") {
  left = first;
  right = second;
} else {
  middle = first;
  left = second;
  right = third;
}

const baseId = useId();
const maskLeftId = `${baseId}-mask-left`;
const maskRightId = `${baseId}-mask-right`;
</script>

<template>
  <svg
    class="venn-diagram"
    width="95"
    height="60"
    viewBox="0 0 95 60"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <!-- Mask for left circle: show left, hide right intersection -->
      <mask :id="maskLeftId">
        <rect width="95" height="60" fill="white" />
        <circle cx="65.5938" cy="30.5" r="28" fill="black" />
      </mask>
      <!-- Mask for right circle: show right, hide left intersection -->
      <mask :id="maskRightId">
        <rect width="95" height="60" fill="white" />
        <circle cx="29.5" cy="30.5" r="28" fill="black" />
      </mask>
    </defs>
    <g :mask="`url(#${maskLeftId})`">
      <Left v-bind="{ ...props, uischema: left }" />
    </g>
    <g :mask="`url(#${maskRightId})`">
      <Right v-bind="{ ...props, uischema: right }" />
    </g>
    <Inner
      v-bind="{ ...props, uischema: middle as unknown as ControlElement }"
    />
  </svg>
</template>

<style scoped>
.venn-diagram {
  margin: var(--kds-spacing-container-0-5x);
}
</style>
