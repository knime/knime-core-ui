<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import {
  Checkbox,
  FunctionButton,
  Label,
  ValueSwitch,
} from "@knime/components";
import {
  SettingsSubPanel,
  type VueControlPropsForLabelContent,
} from "@knime/jsonforms";
import NextArrowIcon from "@knime/styles/img/icons/arrow-next.svg";

import type { FileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import FileBrowserButton from "../FileBrowserButton.vue";
import FileSelectionPreview from "../FileSelectionPreview.vue";
import FilterSettings from "../FilterSettings.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import {
  useFileChooserBrowseOptions,
  useFileChooserFileSystemsOptions,
} from "../composables/useFileChooserBrowseOptions";
import useFileFilterPreviewBackend, {
  type PreviewResult,
} from "../composables/useFileFilterPreviewBackend";
import useSideDrawerContent from "../composables/useSideDrawerContent";
import type { BackendType, MultiFileSelection } from "../types";
import { type FileChooserValue } from "../types/FileChooserProps";

import FSLocationTextControl from "./FSLocationTextControl.vue";
import SideDrawerContent from "./SideDrawerContent.vue";

const props = defineProps<VueControlPropsForLabelContent<MultiFileSelection>>();

const isDisabled = computed(
  () =>
    props.disabled ||
    props.control.uischema.options?.fileSystemConnectionMissing,
);

const selectionMode = computed({
  get: () => props.control.data?.fileOrFolder,
  set: (value) => {
    props.changeValue({ ...props.control.data, fileOrFolder: value });
  },
});

const includeSubFolders = computed({
  get: () => props.control.data?.includeSubfolders,
  set: (value) => {
    props.changeValue({ ...props.control.data, includeSubfolders: value });
  },
});

const onChangePath = (value: FileChooserValue) =>
  props.changeValue({ ...props.control.data, path: value });

const browseOptions = computed(() => {
  return {
    ...props.control.uischema.options,
    selectionMode: selectionMode.value,
  } as FileChooserOptions;
});

const { validCategories } = useFileChooserFileSystemsOptions(browseOptions);

const getDefaultData = () => {
  return {
    path: "",
    timeout: 10000,
    fsCategory: validCategories.value[0],
    context: {
      fsToString: "",
      fsSpecifier: browseOptions.value.fileSystemSpecifier,
    },
  };
};

const data = computed(() => {
  return props.control.data?.path ?? getDefaultData();
});

const { onApply, sideDrawerValue } = useSideDrawerContent<FileChooserValue>({
  onChange: onChangePath,
  initialValue: data,
});

const possibleValueSwitchValues = [
  {
    id: "FILE",
    text: "File",
  },
  {
    id: "FOLDER",
    text: "Folder",
  },
];

const { portIndex } = useFileChooserBrowseOptions(browseOptions);
const backendType = computed<BackendType>(() =>
  getBackendType(data.value.fsCategory, portIndex.value),
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

const selectedPath = computed(() => {
  return props.control.data?.path.path;
});

const refreshPreview = async () => {
  previewDataIsLoading.value = true;
  previewData.value = await backendConnection.listItemsForPreview(
    selectedPath.value,
  );
  previewDataIsLoading.value = false;
};

watch(
  [includeSubFolders, filterOptions, selectedPath, selectionMode],
  () => {
    if (selectionMode.value === "FOLDER") {
      refreshPreview();
    }
  },
  {
    deep: true,
  },
);

onMounted(() => {
  refreshPreview();
});

const filterSettingsRef = ref<typeof FilterSettings | null>(null);

const showGoIntoFolderButton = computed(
  () => sideDrawerValue.value.fsCategory === "LOCAL",
);
</script>

<template>
  <div :id="labelForId" class="flex-column">
    <Label text="Type">
      <ValueSwitch
        id="anotherPotato"
        v-model="selectionMode"
        compact
        :possible-values="possibleValueSwitchValues"
      />
    </Label>
    <Label text="Source" class="flex-column">
      <div class="flex-row">
        <FSLocationTextControl
          class="flex-grow"
          :model-value="data"
          :disabled="isDisabled"
          :is-local="browseOptions.isLocal"
          :is-valid
          :port-index="browseOptions.portIndex"
          :file-system-specifier="browseOptions.fileSystemSpecifier"
          @update:model-value="onChangePath"
        />
        <FileBrowserButton
          :disabled="isDisabled"
          :show-go-into-folder-button
          @apply="onApply"
        >
          <SideDrawerContent
            :id="labelForId ?? null"
            v-model="sideDrawerValue"
            :disabled="isDisabled"
            :options="browseOptions"
            :selection-mode
          />
        </FileBrowserButton>
      </div>
      <Checkbox v-if="selectionMode === 'FOLDER'" v-model="includeSubFolders">
        Include subfolders
      </Checkbox>
      <FileSelectionPreview
        v-if="selectionMode === 'FOLDER'"
        :preview-data
        :is-loading="previewDataIsLoading"
      >
        <template #header-buttons-right>
          <FunctionButton
            class="filter-button"
            @click="filterPanelRef?.expand()"
          >
            Edit filters
            <NextArrowIcon />
          </FunctionButton>
        </template>
      </FileSelectionPreview>
      <SettingsSubPanel
        v-if="selectionMode === 'FOLDER'"
        ref="filterPanelRef"
        show-back-arrow
      >
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
    </Label>
  </div>
</template>

<style lang="postcss" scoped>
.flex-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-8);

  & .flex-grow {
    flex-grow: 1;
  }

  & .fit-content {
    height: fit-content;
  }
}

.flex-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

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
