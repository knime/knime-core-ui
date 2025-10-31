<script setup lang="ts">
import { computed } from "vue";

import { type VueControlProps } from "@knime/jsonforms";

import { fileChooserForMultiFileFormat } from "@/nodeDialog/renderers/fileChooserRenderer";
import type { FileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import FieldControl from "../../FieldControl.vue";
import type { MultiFileSelection } from "../types";

import FilterPreview from "./FilterPreview.vue";

const props = defineProps<VueControlProps<MultiFileSelection>>();
const selectionMode = computed(() => props.control.data?.fileOrFolder);
const browseOptions = computed(() => {
  return {
    ...props.control.uischema.options,
    selectionMode: selectionMode.value,
  } as FileChooserOptions;
});
</script>

<template>
  <div class="flex-column">
    <FieldControl field-name="fileOrFolder" format="valueSwitch" :control />
    <FieldControl
      field-name="path"
      :format="fileChooserForMultiFileFormat"
      :options="browseOptions"
      :control
    />
    <template v-if="selectionMode === 'FOLDER'">
      <FieldControl field-name="includeSubfolders" :control />
      <FilterPreview v-bind="props" />
    </template>
  </div>
</template>

<style lang="postcss" scoped>
.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}
</style>
