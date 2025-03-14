<script lang="ts">
type ModelType = {
  fileFormat: "all_files" | "custom";
  fileExtensions: string[];
  fileNameFilter: "wildcard" | "regex";
  fileNameFilterValue: string;
  includeHiddenFiles: boolean;
  folderFormat: "wildcard" | "regex";
  folderNameFilterValue: string;
  includeHiddenFolders: boolean;
  followLinks: boolean;
};

const defaultModelValue: ModelType = {
  fileFormat: "all_files",
  fileExtensions: [],
  fileNameFilter: "wildcard",
  fileNameFilterValue: "",
  includeHiddenFiles: false,
  folderFormat: "wildcard",
  folderNameFilterValue: "",
  includeHiddenFolders: false,
  followLinks: true,
};
</script>

<script setup lang="ts">
import { computed } from "vue";

import {
  Checkbox,
  ComboBox,
  InputField,
  Label,
  ValueSwitch,
} from "@knime/components";
import { SectionHeading } from "@knime/jsonforms";

const filterSettingsModel = defineModel<ModelType>({
  required: false,
  default: { ...defaultModelValue },
});

defineExpose({
  resetFilters: () => {
    filterSettingsModel.value = defaultModelValue;
  },
});

const fileFormat = computed<ModelType["fileFormat"]>({
  get: () => filterSettingsModel.value.fileFormat,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      fileFormat: value,
    };
  },
});
const fileFormatOptions = [
  { id: "all_files", text: "All files" },
  { id: "custom", text: "Custom" },
];

const fileExtensions = computed<ModelType["fileExtensions"]>({
  get: () => filterSettingsModel.value.fileExtensions,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      fileExtensions: value,
    };
  },
});
const fileExtensionsPossibleOptions = computed(() =>
  fileExtensions.value.map((ext) => ({ id: ext, text: ext })),
);

const fileNameFilter = computed<ModelType["fileNameFilter"]>({
  get: () => filterSettingsModel.value.fileNameFilter,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      fileNameFilter: value,
    };
  },
});
const fileNameFilterOptions = [
  { id: "wildcard", text: "Wildcard" },
  { id: "regex", text: "Regular Expression" },
];
const fileNameFilterValue = computed<ModelType["fileNameFilterValue"]>({
  get: () => filterSettingsModel.value.fileNameFilterValue,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      fileNameFilterValue: value,
    };
  },
});

const includeHiddenFiles = computed<ModelType["includeHiddenFiles"]>({
  get: () => filterSettingsModel.value.includeHiddenFiles,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      includeHiddenFiles: value,
    };
  },
});

const folderFormat = computed<ModelType["folderFormat"]>({
  get: () => filterSettingsModel.value.folderFormat,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      folderFormat: value,
    };
  },
});

const folderNameFilterValue = computed<ModelType["folderNameFilterValue"]>({
  get: () => filterSettingsModel.value.folderNameFilterValue,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      folderNameFilterValue: value,
    };
  },
});

const includeHiddenFolders = computed<ModelType["includeHiddenFolders"]>({
  get: () => filterSettingsModel.value.includeHiddenFolders,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      includeHiddenFolders: value,
    };
  },
});

const followLinks = computed<ModelType["followLinks"]>({
  get: () => filterSettingsModel.value.followLinks,
  set: (value) => {
    filterSettingsModel.value = {
      ...filterSettingsModel.value,
      followLinks: value,
    };
  },
});
</script>

<template>
  <div class="container">
    <SectionHeading title-text="File Filters" class="section-heading" />

    <Label text="File format">
      <div class="grouped-controls">
        <ValueSwitch
          v-model="fileFormat"
          compact
          :possible-values="fileFormatOptions"
        />
      </div>
    </Label>

    <Label text="File extensions">
      <div class="grouped-controls">
        <ComboBox
          v-model="fileExtensions"
          compact
          :possible-values="fileExtensionsPossibleOptions"
          allow-new-values
        />
      </div>
    </Label>

    <Label text="File name">
      <div class="grouped-controls">
        <ValueSwitch
          v-model="fileNameFilter"
          compact
          :possible-values="fileNameFilterOptions"
        />
        <InputField v-model="fileNameFilterValue" compact />
      </div>
    </Label>

    <Checkbox v-model="includeHiddenFiles">Include hidden files</Checkbox>

    <SectionHeading title-text="Folder Filters" class="section-heading" />

    <Label text="Folder format">
      <div class="grouped-controls">
        <ValueSwitch
          v-model="folderFormat"
          compact
          :possible-values="fileNameFilterOptions"
        />
        <InputField v-model="folderNameFilterValue" compact />
      </div>
    </Label>

    <Checkbox v-model="includeHiddenFolders">Include hidden folders</Checkbox>

    <SectionHeading title-text="Links" class="section-heading" />

    <Checkbox v-model="followLinks">Follow links</Checkbox>
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
