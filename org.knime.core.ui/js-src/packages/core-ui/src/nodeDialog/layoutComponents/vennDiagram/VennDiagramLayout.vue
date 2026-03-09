<script setup lang="ts">
import type { ControlElement } from "@jsonforms/core";
import { DispatchRenderer, rendererProps } from "@jsonforms/vue";

import { VerticalLayoutBase } from "@knime/jsonforms";

import VennDiagram from "./VennDiagram.vue";

const props = defineProps(
  rendererProps<{
    elements:
      | [left: ControlElement, middle: ControlElement, right: ControlElement]
      | [left: ControlElement, right: ControlElement];
  }>(),
);
</script>

<template>
  <div class="horizontal">
    <VerticalLayoutBase #default="{ element }" :elements="uischema.elements">
      <DispatchRenderer v-bind="props" :uischema="element as ControlElement" />
    </VerticalLayoutBase>
    <VerticalLayoutBase :elements="[uischema.elements[0]]" class="flex-none">
      <VennDiagram v-bind="props" />
    </VerticalLayoutBase>
  </div>
</template>

<style lang="postcss" scoped>
.flex-none {
  flex: 0;
}

.horizontal {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
}
</style>
