<script setup lang="ts">
import { rendererProps } from "@jsonforms/vue";
import { optionsMapper } from "../utils";
import DropdownControl from "./DropdownControl.vue";
import { useJsonFormsControlWithUpdate } from "../composables/components/useJsonFormsControlWithUpdate";

const props = defineProps(rendererProps());
const { control } = useJsonFormsControlWithUpdate(props);
const options = control.value.schema?.oneOf?.map(optionsMapper) ?? [];
const asyncInitialOptions = Promise.resolve(options);
</script>

<template>
  <DropdownControl
    v-bind="{ ...$attrs, ...$props }"
    :async-initial-options="asyncInitialOptions"
  />
</template>
