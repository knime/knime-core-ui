<script setup lang="ts">
import { computed, ref, watch } from "vue";

import { FunctionButton } from "@knime/components";
import {
  SettingsSubPanel,
  type VueControlPropsForLabelContent,
} from "@knime/jsonforms";
import NextArrowIcon from "@knime/styles/img/icons/arrow-next.svg";

import FileSelectionPreview from "../FileSelectionPreview.vue";
import FilterSettings from "../FilterSettings.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useFileFilterPreviewBackend, {
  type PreviewResult,
} from "../composables/useFileFilterPreviewBackend";
import type { BackendType, MultiFileSelection } from "../types";

const props = defineProps<VueControlPropsForLabelContent<MultiFileSelection>>();
const includeSubFolders = computed(() => props.control.data?.includeSubfolders);
const browseOptions = computed(() => props.control.uischema.options!);

const { portIndex } = useFileChooserBrowseOptions(browseOptions);
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
  additionalFilterOptionsClassIdentifier:
    props.control.uischema.options?.additionalFilterOptionsClassIdentifier,
});

const selectedPath = computed(() => props.control.data.path.path);

const refreshPreview = async () => {
  previewDataIsLoading.value = true;
  previewData.value = await backendConnection.listItemsForPreview(
    selectedPath.value,
  );
  previewDataIsLoading.value = false;
};

watch(
  [includeSubFolders, filterOptions, selectedPath],
  () => refreshPreview(),
  {
    deep: true,
    immediate: true,
  },
);

const filterSettingsRef = ref<typeof FilterSettings | null>(null);
</script>

<template>
  <FileSelectionPreview :preview-data :is-loading="previewDataIsLoading">
    <template #header-buttons-right>
      <FunctionButton class="filter-button" @click="filterPanelRef?.expand()">
        Edit filters
        <NextArrowIcon />
      </FunctionButton>
    </template>
  </FileSelectionPreview>
  <SettingsSubPanel ref="filterPanelRef" show-back-arrow>
    <div class="filter-settings-drawer-content">
      <FileSelectionPreview
        :preview-data
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
