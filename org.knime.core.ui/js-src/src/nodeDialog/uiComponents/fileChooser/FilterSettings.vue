<script lang="ts">
import type { FilterOptions } from "./composables/useFileFilterPreviewBackend";

export type ModelType = Omit<FilterOptions, "includeSubFolders">;
</script>

<script setup lang="ts">
import { computed, onMounted, ref, toRaw } from "vue";

import {
  Checkbox,
  ComboBox,
  InputField,
  Label,
  ValueSwitch,
} from "@knime/components";
import { SectionHeading } from "@knime/jsonforms";

const filterSettingsModel = defineModel<ModelType>({
  required: true,
});

const defaultModelValue = ref<ModelType | null>(null);

onMounted(() => {
  defaultModelValue.value = toRaw(filterSettingsModel.value);
});

defineExpose({
  resetFilters: () => {
    filterSettingsModel.value = defaultModelValue.value!;
  },
});

const fileFormatOptions = [
  { id: "all", text: "All files" },
  { id: "custom", text: "Custom" },
];

const fileExtensionsPossibleOptions = computed(() =>
  filterSettingsModel.value.fileExtensions.map((ext) => ({
    id: ext,
    text: ext,
  })),
);

const fileNameFilterOptions = [
  { id: "all_pass", text: "All files" },
  { id: "wildcard", text: "Wildcard" },
  { id: "regex", text: "Regular Expression" },
];

const folderNameFilterOptions = [
  { id: "all_pass", text: "All folders" },
  { id: "wildcard", text: "Wildcard" },
  { id: "regex", text: "Regular Expression" },
];

const caseSensitivityOptions = [
  { id: "case_sensitive", text: "Case sensitive" },
  { id: "case_insensitive", text: "Case insensitive" },
];
</script>

<template>
  <div class="container">
    <SectionHeading title-text="File Filters" class="section-heading" />

    <Label text="File extensions">
      <div class="grouped-controls">
        <ValueSwitch
          :model-value="filterSettingsModel.fileFormat"
          compact
          :possible-values="fileFormatOptions"
          @update:model-value="
            (newValue) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                fileFormat: newValue,
              })
          "
        />
        <ComboBox
          v-if="filterSettingsModel.fileFormat === 'custom'"
          :model-value="filterSettingsModel.fileExtensions"
          compact
          :possible-values="fileExtensionsPossibleOptions"
          allow-new-values
          @update:model-value="
            (newValue) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                fileExtensions: newValue as string[],
              })
          "
        />
      </div>
    </Label>

    <Label text="File name">
      <div class="grouped-controls">
        <ValueSwitch
          :model-value="filterSettingsModel.filenamePatternType"
          compact
          :possible-values="fileNameFilterOptions"
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                filenamePatternType: newValue as 'wildcard' | 'regex',
              })
          "
        />
        <InputField
          v-if="filterSettingsModel.filenamePatternType !== 'all_pass'"
          :model-value="filterSettingsModel.filenamePattern"
          compact
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                filenamePattern: newValue,
              })
          "
        />
        <ValueSwitch
          :model-value="
            filterSettingsModel.filenamePatternCaseSensitive
              ? 'case_sensitive'
              : 'case_insensitive'
          "
          compact
          :possible-values="caseSensitivityOptions"
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                filenamePatternCaseSensitive: newValue === 'case_sensitive',
              })
          "
        />
      </div>
    </Label>

    <Checkbox
      :model-value="filterSettingsModel.includeHiddenFiles"
      @update:model-value="
        (newValue: boolean) =>
          (filterSettingsModel = {
            ...filterSettingsModel,
            includeHiddenFiles: newValue,
          })
      "
    >
      Include hidden files
    </Checkbox>

    <SectionHeading title-text="Folder Filters" class="section-heading" />

    <Label text="Folder format">
      <div class="grouped-controls">
        <ValueSwitch
          :model-value="filterSettingsModel.folderNamePatternType"
          compact
          :possible-values="folderNameFilterOptions"
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                folderNamePatternType: newValue as 'wildcard' | 'regex',
              })
          "
        />
        <InputField
          v-if="filterSettingsModel.folderNamePatternType !== 'all_pass'"
          :model-value="filterSettingsModel.folderNamePattern"
          compact
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                folderNamePattern: newValue,
              })
          "
        />
        <ValueSwitch
          :model-value="
            filterSettingsModel.folderNamePatternCaseSensitive
              ? 'case_sensitive'
              : 'case_insensitive'
          "
          compact
          :possible-values="caseSensitivityOptions"
          @update:model-value="
            (newValue: string) =>
              (filterSettingsModel = {
                ...filterSettingsModel,
                folderNamePatternCaseSensitive: newValue === 'case_sensitive',
              })
          "
        />
      </div>
    </Label>

    <Checkbox
      :model-value="filterSettingsModel.includeHiddenFolders"
      @update:model-value="
        (newValue: boolean) =>
          (filterSettingsModel = {
            ...filterSettingsModel,
            includeHiddenFolders: newValue,
          })
      "
    >
      Include hidden folders
    </Checkbox>

    <SectionHeading title-text="Links" class="section-heading" />

    <Checkbox
      :model-value="filterSettingsModel.followLinks"
      @update:model-value="
        (newValue: boolean) =>
          (filterSettingsModel = {
            ...filterSettingsModel,
            followLinks: newValue,
          })
      "
      >Follow links</Checkbox
    >
  </div>
</template>

<style lang="postcss" scoped>
.section-heading {
  background-color: var(--knime-porcelain);
}

.container {
  display: flex;
  flex-direction: column;
  gap: var(--space-16);
}

.grouped-controls {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}
</style>
