<script setup lang="ts">
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
</script>

<template>
  <svg
    width="95"
    height="60"
    viewBox="0 0 95 60"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <Left v-bind="{ ...props, uischema: left }" />
    <Right v-bind="{ ...props, uischema: right }" />
    <Inner
      v-bind="{ ...props, uischema: middle as unknown as ControlElement }"
    />
  </svg>
</template>
