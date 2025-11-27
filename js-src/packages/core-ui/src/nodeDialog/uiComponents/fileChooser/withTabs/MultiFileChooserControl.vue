<script setup lang="ts">
import { computed } from "vue";

import { type VueControlProps } from "@knime/jsonforms";

import { fileChooserForMultiFileFormat } from "@/nodeDialog/renderers/fileChooserRenderer";
import type {
  FileChooserOptions,
  MultiFileChooserUiSchema,
  MultiFileFilterMode,
} from "@/nodeDialog/types/FileChooserUiSchema";
import FieldControl from "../../FieldControl.vue";
import { useFileSystems } from "../composables/useFileChooserBrowseOptions";
import type { MultiFileSelection } from "../types";

import FilterPreview from "./FilterPreview.vue";

const props = defineProps<VueControlProps<MultiFileSelection>>();

const uiSchema = computed(
  () => props.control.uischema as MultiFileChooserUiSchema,
);

const selectionMode = computed(() => {
  const filterMode = props.control.data?.filterMode;
  return filterMode === "FILE" ? "FILE" : "FOLDER";
});

const showFilters = computed(() => {
  const filterMode = props.control.data?.filterMode;
  return filterMode !== "FILE" && filterMode !== "FOLDER";
});

const pathSelectionOptions = computed<FileChooserOptions>(() => ({
  ...uiSchema.value.options,
  selectionMode: selectionMode.value,
}));

const filterModeLabels: Record<MultiFileFilterMode, string> = {
  FILE: "File",
  FOLDER: "Folder",
  FILES_AND_FOLDERS: "Files and folders",
  FILES_IN_FOLDERS: "Files in folders",
  FOLDERS: "Folders",
};

const filterModeToPossibleValue = (
  mode: MultiFileFilterMode,
): {
  id: MultiFileFilterMode;
  text: string;
} => ({
  id: mode,
  text: filterModeLabels[mode],
});

const possibleFilterModes = computed(
  () =>
    uiSchema.value.options?.possibleFilterModes?.map(
      filterModeToPossibleValue,
    ) ?? [],
);
const filterModeOptions = computed(() => ({
  possibleValues: possibleFilterModes.value,
}));
const showFilterMode = computed(() => possibleFilterModes.value.length > 1);
const filterModeFormat = computed(() =>
  possibleFilterModes.value.length > 2 ? "radio" : "valueSwitch",
);
const { isConnectedButNoFileConnectionIsAvailable, isConnected } =
  useFileSystems(pathSelectionOptions);
const hideFilterPreview = computed(
  () =>
    isConnectedButNoFileConnectionIsAvailable.value ||
    (!isConnected.value && props.control.data?.path.fsCategory === "CONNECTED"),
);
</script>

<template>
  <div class="flex-column">
    <FieldControl
      v-if="showFilterMode"
      field-name="filterMode"
      :format="filterModeFormat"
      :options="filterModeOptions"
      :control
    />
    <FieldControl
      field-name="path"
      :format="fileChooserForMultiFileFormat"
      :options="pathSelectionOptions"
      :control
    />
    <template v-if="showFilters">
      <FieldControl field-name="includeSubfolders" :control />
      <FilterPreview v-if="!hideFilterPreview" v-bind="props" />
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
