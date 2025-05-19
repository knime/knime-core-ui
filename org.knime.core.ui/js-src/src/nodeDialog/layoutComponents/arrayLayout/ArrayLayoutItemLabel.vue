<script lang="ts">
export type TitleConfig =
  | { type: "provided" }
  | { type: "enumerated"; title: string };
/**
 * This scope is handled specially in addIndexToStateProviders.
 */
export const topLevelElementScope = "topLevelElementScope";
</script>

<script setup lang="ts">
import { computed } from "vue";

import { Label } from "@knime/components";
import { useProvidedState } from "@knime/jsonforms";

import type { ArrayLayoutControl } from "./ArrayLayout.vue";

const props = defineProps<{
  arrayUiSchema: ArrayLayoutControl["uischema"];
  titleConfig: TitleConfig;
  index: number;
}>();

const titleConfig = props.titleConfig;
/**
 * In this special case, we need the options and providedOptions properties of the uischema of the array
 * layout but we do not want the scope to take effect since it would be concatenated with itself as we are
 * inside an array layout element here.
 */
const topLevelElementUiSchema = computed(() => ({
  ...props.arrayUiSchema,
  scope: topLevelElementScope,
}));
const elementTitle =
  titleConfig.type === "provided"
    ? useProvidedState(topLevelElementUiSchema, "arrayElementTitle", "")
    : computed(() => `${titleConfig.title} ${props.index + 1}`);

const subTitle = useProvidedState(
  topLevelElementUiSchema,
  "elementSubTitle",
  "",
);
</script>

<template>
  <div class="vertical">
    <Label :text="elementTitle" compact />
    <span v-if="subTitle">{{ subTitle }}</span>
  </div>
</template>

<style scoped>
.vertical {
  display: flex;
  flex-direction: column;

  & span {
    font-style: italic;
    font-size: 10px;
  }
}
</style>
