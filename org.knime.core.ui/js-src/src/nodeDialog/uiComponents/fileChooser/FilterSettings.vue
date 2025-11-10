<script setup lang="ts">
import { computed } from "vue";

import { type VueControlProps } from "@knime/jsonforms";

import type { MultiFileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import { composePaths } from "@/nodeDialog/utils/paths";
import FieldRenderer from "../FieldRenderer.vue";

import type { FilterOptions } from "./composables/useFileFilterPreviewBackend";

const props = defineProps<{
  control: VueControlProps<FilterOptions>["control"];
  handleChange: (path: string, newValue: unknown) => void;
}>();

const FILTERS_PATH = "filters";
const path = computed(() => composePaths(props.control.path, FILTERS_PATH));
const uischema = computed(
  () =>
    (props.control.uischema.options as MultiFileChooserOptions)?.filters
      .uiSchema,
);

defineExpose({
  /**
   * Resets the filter settings to their default values. NOT the same as discarding them
   * and returning to the saved state.
   */
  resetFilters: () => {
    props.handleChange(path.value, {
      ...props.control.schema.default?.[FILTERS_PATH],
      filterMode: props.control.data.filters.filterMode,
    });
  },
});
</script>

<template>
  <FieldRenderer
    :control="control"
    :field-name="FILTERS_PATH"
    :handle-change="handleChange"
    :uischema="uischema"
  />
</template>
