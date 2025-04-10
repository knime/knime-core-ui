<script setup lang="ts">
import { type Ref, computed, ref, toRefs, watch } from "vue";

import {
  Breadcrumb,
  FileExplorer,
  type FileExplorerItem,
  InputField,
  LoadingIcon,
} from "@knime/components";
import HouseIcon from "@knime/styles/img/icons/house.svg";

import useFileChooserBackend from "./composables/useFileChooserBackend";
import type {
  BackendType,
  Folder,
  FolderAndError,
  ParentFolder,
} from "./types";
import { toFileExplorerItem } from "./utils";

export interface DialogFileExplorerProps {
  initialFilePath?: string;
  isWriter?: boolean;
  filteredExtensions?: string[];
  appendedExtension?: string | null;
  backendType: BackendType;
  clickOutsideExceptions?: Ref<null | HTMLElement>[];
  openFileByExplorer?: boolean;
  breadcrumbRoot?: string | null;
  selectionMode?: "FILE" | "FOLDER";
}

const currentPath = ref<string | null>(null);
const currentParents = ref<ParentFolder[]>([]);

const props = withDefaults(defineProps<DialogFileExplorerProps>(), {
  initialFilePath: "",
  isWriter: false,
  filteredExtensions: () => [],
  appendedExtension: null,
  clickOutsideExceptions: () => [],
  openFileByExplorer: false,
  breadcrumbRoot: null,
  selectionMode: "FILE",
});

export type SelectedItem = {
  name: string;
  selectionType: DialogFileExplorerProps["selectionMode"];
} | null;

const selectedItem = defineModel<SelectedItem>("selectedItem", {
  required: false,
  default: null,
});

const allItemsInCurrentFolder = ref<FileExplorerItem[]>([]);

// TODO(UIEXT-2660): we need to make these items disabled, not hidden. Currently
// this isn't nicely supported by the WAC component, so we will push
// it to a follow-up ticket.
const displayedItemsInCurrentFolder = computed(() =>
  allItemsInCurrentFolder.value.filter(
    (item) => props.selectionMode === "FILE" || item.isDirectory,
  ),
);

const breadcrumbItems = computed(() =>
  currentParents.value.map((parent, index) => {
    let text = parent.name;
    let icon = null;
    if (typeof text !== "string") {
      if (parent.path === null) {
        if (props.breadcrumbRoot === null) {
          icon = HouseIcon;
        } else {
          text = props.breadcrumbRoot;
        }
      } else {
        text = parent.path;
      }
    }
    return {
      ...(text === null ? {} : { text }),
      title: text ?? "Root",
      path: parent.path,
      clickable: index !== currentParents.value.length - 1,
      ...(icon ? { icon } : {}),
    };
  }),
);

const emit = defineEmits<{
  chooseItem: [
    /**
     * The full path of the chosen file
     */
    filePath: string,
  ];
}>();

const isLoading = ref(true);

const setNextItems = (folder: Folder) => {
  isLoading.value = false;
  currentPath.value = folder.path;
  currentParents.value = folder.parentFolders;

  allItemsInCurrentFolder.value = folder.items.map(toFileExplorerItem);
};

const displayedError = ref<string | null>(null);
const setErrorMessage = (errorMessage: string | undefined) => {
  displayedError.value = errorMessage ?? null;
};

const selectedFileName = ref("");

const setRelativeFilePathFromBackend = (filePathRelativeToFolder: string) => {
  if (props.isWriter) {
    selectedFileName.value = filePathRelativeToFolder;
  }
};

const handleListItemsResult = (folderAndError: FolderAndError) => {
  setNextItems(folderAndError.folder);
  setErrorMessage(folderAndError.errorMessage);
  setRelativeFilePathFromBackend(folderAndError.filePathRelativeToFolder);
};

const { filteredExtensions, appendedExtension, isWriter, backendType } =
  toRefs(props);

const { listItems, getFilePath } = useFileChooserBackend({
  filteredExtensions,
  appendedExtension,
  isWriter,
  backendType,
});

watch(
  () => backendType.value,
  () => listItems(null, props.initialFilePath).then(handleListItemsResult),
  { immediate: true },
);

const selectedDirectoryName = ref("");

watch([selectedFileName, selectedDirectoryName], (selectedItems) => {
  // Only one of the two can be selected at a time
  const somethingIsSelected = selectedItems.some((item) => item !== "");

  if (somethingIsSelected) {
    selectedItem.value = {
      name: selectedItems[0] || selectedItems[1],
      selectionType: selectedItems[0] ? "FILE" : "FOLDER",
    };
  } else {
    selectedItem.value = null;
  }
});
watch(
  () => props.selectionMode,
  () => {
    selectedDirectoryName.value = "";
    selectedFileName.value = "";
  },
);

const loadNewFolder = (
  path: string | null,
  folderName: string | null = null,
) => {
  selectedDirectoryName.value = "";
  selectedFileName.value = "";
  isLoading.value = true;
  listItems(path, folderName).then(handleListItemsResult);
};

const changeDirectory = (nextFolder: string) =>
  loadNewFolder(currentPath.value, nextFolder);

const onBreadcrumbItemClick = ({ path }: { path?: string | null }) =>
  loadNewFolder(path ?? null);

const onChooseItem = async (name: string) => {
  const { path, errorMessage } = await getFilePath(currentPath.value, name);
  if (path === null) {
    setErrorMessage(errorMessage);
    return Promise.reject(errorMessage);
  } else {
    emit("chooseItem", path);
    return Promise.resolve();
  }
};

// called by FileExplorerTab when apply button is clicked
defineExpose({
  chooseSelectedItem: () =>
    onChooseItem(
      props.selectionMode === "FILE"
        ? selectedFileName.value
        : selectedDirectoryName.value,
    ),
  goIntoSelectedFolder: () => {
    changeDirectory(selectedDirectoryName.value);
  },
});

const onChangeSelectedItemIds = (itemIds: string[]) => {
  if (itemIds.length === 0) {
    selectedDirectoryName.value = "";
    if (!props.isWriter) {
      selectedFileName.value = "";
    }
    return;
  }
  const newSelectedItem = displayedItemsInCurrentFolder.value.find(
    ({ id }) => id === itemIds[0],
  )!;
  if (newSelectedItem.isDirectory) {
    selectedDirectoryName.value = newSelectedItem.name;
    selectedFileName.value = "";
  } else {
    selectedDirectoryName.value = "";
    selectedFileName.value = newSelectedItem.name;
  }
};
</script>

<template>
  <div v-if="isLoading" class="loading-animaton">
    <LoadingIcon class="icon" />
  </div>
  <template v-else>
    <div class="current-path">
      <Breadcrumb
        class="breadcrumb"
        :items="breadcrumbItems"
        no-wrap
        compact
        @click-item="onBreadcrumbItemClick as any"
      />
      <span v-if="displayedError !== null" class="error"
        >({{ displayedError }})</span
      >
    </div>
    <div v-if="isWriter" class="name-input-wrapper">
      <span>Name:</span>
      <InputField v-model="selectedFileName" />
    </div>
    <FileExplorer
      class="explorer"
      :is-root-folder="currentPath === null"
      :items="displayedItemsInCurrentFolder"
      :disable-context-menu="true"
      :disable-multi-select="true"
      :disable-dragging="true"
      :click-outside-exception="clickOutsideExceptions"
      @change-directory="changeDirectory"
      @open-file="
        openFileByExplorer && onChooseItem($event.name).catch(() => {})
      "
      @update:selected-item-ids="onChangeSelectedItemIds"
    />
  </template>
</template>

<style scoped lang="postcss">
.loading-animaton {
  align-items: center;
  flex: 1;
  display: flex;
  min-height: 0;
  flex-direction: column;
  justify-content: center;

  & .icon {
    width: 15px;
    height: 15px;
  }
}

.current-path {
  font-size: 13px;
  display: flex;
  margin: 5px;
  flex-direction: column;

  & .breadcrumb {
    width: 100%;
    user-select: none;
  }

  & span.error {
    color: var(--theme-color-error);
  }
}

.name-input-wrapper {
  display: flex;
  font-size: 13px;
  flex-direction: row;
  gap: 10px;
  align-items: baseline;
  margin: 5px;
}

.explorer {
  min-height: 0;
  overflow-y: auto;
  flex: 1;
}
</style>
