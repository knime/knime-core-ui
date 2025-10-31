<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { composePaths } from "@jsonforms/core";

import { FunctionButton } from "@knime/components";
import { SettingsSubPanel, type VueControlProps } from "@knime/jsonforms";
import NextArrowIcon from "@knime/styles/img/icons/arrow-next.svg";

import type {
  FileChooserUiSchema,
  MultiFileChooserOptions,
} from "@/nodeDialog/types/FileChooserUiSchema";
import FileSelectionPreview from "../FileSelectionPreview.vue";
import FilterSettings from "../FilterSettings.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useFileFilterPreviewBackend, {
  type PreviewResult,
} from "../composables/useFileFilterPreviewBackend";
import type { BackendType, MultiFileSelection } from "../types";

const props = defineProps<VueControlProps<MultiFileSelection>>();
const includeSubFolders = computed(() => props.control.data?.includeSubfolders);
const uischema = computed<FileChooserUiSchema>(
  () => props.control.uischema as FileChooserUiSchema,
);

const { portIndex } = useFileChooserBrowseOptions(uischema);
const backendType = computed<BackendType>(() =>
  getBackendType(props.control.data.path.fsCategory, portIndex.value),
);

const previewData = ref<PreviewResult>({
  itemsAfterFiltering: [],
  numItemsBeforeFiltering: 0,
  numFilesAfterFilteringIsOnlyLowerBound: false,
  numFilesBeforeFilteringIsOnlyLowerBound: false,
  resultType: "SUCCESS",
});
const previewDataIsLoading = ref(true);

const filterPanelRef = ref<typeof SettingsSubPanel | null>(null);

const filterOptions = computed(() => props.control.data?.filters);
const backendConnection = useFileFilterPreviewBackend({
  backendType,
  includeSubFolders,
  filterOptions,
  additionalFilterOptionsClassIdentifier: (
    props.control.uischema.options as MultiFileChooserOptions
  )?.filters.classId,
});

const selectedPath = computed(() => props.control.data.path.path);
const filterMode = computed(() => props.control.data.filterMode);

const mostRecentPreviewQueryId = ref(0);
const refreshPreview = async () => {
  previewDataIsLoading.value = true;

  ++mostRecentPreviewQueryId.value;
  const currentQueryId = mostRecentPreviewQueryId.value;

  const previewResult = await backendConnection.listItemsForPreview(
    selectedPath.value,
    filterMode.value,
  );

  if (currentQueryId === mostRecentPreviewQueryId.value) {
    previewData.value = previewResult;
    previewDataIsLoading.value = false;
  }
};

watch(
  filterMode,
  (newMode) => {
    if (props.control.data.filters.filterMode !== newMode) {
      props.handleChange(
        composePaths(composePaths(props.control.path, "filters"), "filterMode"),
        newMode,
      );
    }
  },
  { immediate: true },
);

watch(
  [includeSubFolders, filterOptions, selectedPath],
  () => refreshPreview(),
  {
    deep: true,
  },
);

const filterSettingsRef = ref<typeof FilterSettings | null>(null);
</script>

<template>
  <FileSelectionPreview
    :preview-data
    :is-loading="previewDataIsLoading"
    :filter-mode
  >
    <template #header-buttons-right>
      <FunctionButton class="filter-button" @click="filterPanelRef?.expand()">
        Edit filters
        <NextArrowIcon />
      </FunctionButton>
    </template>
  </FileSelectionPreview>
  <SettingsSubPanel
    ref="filterPanelRef"
    show-back-arrow
    background-color-override="var(--knime-gray-ultra-light)"
  >
    <div class="filter-settings-drawer-content">
      <FileSelectionPreview
        :preview-data
        :filter-mode
        :is-loading="previewDataIsLoading"
        expand-by-default
      >
        <template #header-buttons-right>
          <FunctionButton
            class="filter-button reset-button"
            @click="filterSettingsRef?.resetFilters()"
          >
            Reset all filters
          </FunctionButton>
        </template>
      </FileSelectionPreview>
      <FilterSettings ref="filterSettingsRef" :control :handle-change />
    </div>
  </SettingsSubPanel>
</template>

<style lang="postcss" scoped>
.filter-settings-drawer-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.filter-button {
  display: flex;
  text-wrap: nowrap;
  flex-grow: 0;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  font-family: Roboto, sans-serif;
  color: var(--color-primary);
  cursor: pointer;

  & svg {
    width: 16px;
    height: 16px;
  }

  &.reset-button {
    color: var(--knime-coral-dark);
  }
}
</style>
