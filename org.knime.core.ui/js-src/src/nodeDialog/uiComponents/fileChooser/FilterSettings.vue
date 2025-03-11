<script setup lang="ts">
import { computed } from "vue";
import type { UISchemaElement } from "@jsonforms/core";
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps } from "@knime/jsonforms";

import { composePaths } from "@/nodeDialog/utils/paths";

const props = defineProps<{
  control: VueControlProps<any>["control"];
  uischema: UISchemaElement;
  handleChangeFunction: (path: string, newValue: any) => void;
}>();

const FILTERS_PATH = "filters";
const path = computed(() => composePaths(props.control.path, FILTERS_PATH));
const schema = computed(() => props.control.schema.properties?.[FILTERS_PATH]!);

defineExpose({
  /**
   * Resets the filter settings to their default values. NOT the same as discarding them
   * and returning to the saved state.
   */
  resetFilters: () => {
    props.handleChangeFunction(
      path.value,
      props.control.schema.default?.[FILTERS_PATH],
    );
  },
});
</script>

<template>
  <DispatchRenderer
    :key="path"
    :schema
    :uischema
    :path
    :enabled="control.enabled"
    :renderers="control.renderers"
    :cells="control.cells"
  />
</template>
