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
import { useFlowSettings } from "../../../composables/components/useFlowVariables";
import FileBrowserButton from "../FileBrowserButton.vue";
import FileSelectionPreview from "../FileSelectionPreview.vue";
import FilterSettings from "../FilterSettings.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import {
  useFileChooserBrowseOptions,
  useFileChooserFileSystemsOptions,
} from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
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
  props.changeValue({ ...props.control.data, root: value });

const { flowSettings } = useFlowSettings({
  path: computed(() => props.control.path),
});

const isOverwritten = computed(() =>
  Boolean(flowSettings.value?.controllingFlowVariableName),
);

const browseOptions = computed(() => {
  return {
    ...props.control.uischema.options,
    selectionMode: selectionMode.value,
  } as FileChooserOptions;
});

const { onFsCategoryUpdate } = useFileChooserStateChange(
  computed(() => props.control.data?.root),
  onChangePath,
  browseOptions,
);

const { validCategories } = useFileChooserFileSystemsOptions(browseOptions);

/**
 * This currently can happen in case a node implementation sets the default value to one that is not supported in this frontend.
 * Or when there was a file system connected/removed since the last time the settings were saved.
 * In this case, we switch to a default.
 */
onMounted(() => {
  if (
    !isOverwritten.value &&
    !validCategories.value.includes(props.control.data?.root.fsCategory)
  ) {
    onFsCategoryUpdate(validCategories.value[0]);
  }
});

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

/**
 * Reset to default data when flow variable is cleared
 */
watch(
  () => isOverwritten.value,
  (value) => {
    if (!value) {
      onChangePath(getDefaultData());
    }
  },
);

const data = computed(() => {
  return props.control.data?.root ?? getDefaultData();
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

const { filteredExtensions, isWriter, portIndex } =
  useFileChooserBrowseOptions(browseOptions);
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
  filteredExtensions,
  isWriter: ref(isWriter.value!),
  backendType,
  includeSubFolders,
  filterOptions,
});

const selectedPath = computed(() => {
  return props.control.data?.root.path;
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
</script>

<template>
  <div :id="labelForId" class="flex-column">
    <Label text="Type">
      <ValueSwitch
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
        <FileBrowserButton :disabled="isDisabled" @apply="onApply">
          <SideDrawerContent
            :id="labelForId ?? null"
            v-model="sideDrawerValue"
            :disabled="isDisabled"
            :options="browseOptions"
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
          <FilterSettings
            ref="filterSettingsRef"
            :uischema="control.uischema.options?.filterSubUiSchema"
            :control
            :handle-change-function="handleChange"
          />
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
