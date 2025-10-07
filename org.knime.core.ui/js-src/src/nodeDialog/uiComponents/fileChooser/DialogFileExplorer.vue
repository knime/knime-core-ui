<script setup lang="ts">
import { computed, ref, toRef, toRefs, watch } from "vue";

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
import { useDialogFileExplorerButtons } from "./withTabs/useDialogFileExplorerButtons";

export interface DialogFileExplorerProps {
  initialFilePath?: string;
  isWriter?: boolean;
  filteredExtensions?: string[];
  appendedExtension?: string | null;
  backendType: BackendType;
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
  breadcrumbRoot: null,
  selectionMode: "FILE",
});

export type SelectedItem = {
  name: string;
  selectionType: "FILE" | "FOLDER";
} | null;

const selectedItem = ref<SelectedItem>(null);

const allItemsInCurrentFolder = ref<FileExplorerItem[]>([]);

const displayedItemsInCurrentFolder = computed(() =>
  allItemsInCurrentFolder.value.map((item) => ({
    ...item,
    ...{
      disabled:
        item.disabled ||
        (props.selectionMode === "FOLDER" && !item.isDirectory),
    },
  })),
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
  applyAndClose: [];
  cancel: [];
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

const setRelativeFilePathFromBackend = (filePathRelativeToFolder: string) => {
  if (props.isWriter && props.selectionMode === "FILE") {
    selectedItem.value = {
      name: filePathRelativeToFolder,
      selectionType: "FILE",
    };
  }
};

const handleListItemsResult = (folderAndError: FolderAndError | undefined) => {
  if (!folderAndError) {
    setErrorMessage("Failed to load items");
    emit("cancel");
    return;
  }
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

const selectedFileName = computed(() =>
  selectedItem.value?.selectionType === "FILE" ? selectedItem.value?.name : "",
);

const loadNewFolder = (
  path: string | null,
  folderName: string | null = null,
) => {
  selectedItem.value = null;
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

const onChangeSelectedItemIds = (itemIds: string[]) => {
  if (itemIds.length === 0) {
    selectedItem.value = null;
    return;
  }
  const newSelectedItem = displayedItemsInCurrentFolder.value.find(
    ({ id }) => id === itemIds[0],
  )!;
  if (newSelectedItem.isDirectory) {
    selectedItem.value = {
      name: newSelectedItem.name,
      selectionType: "FOLDER",
    };
  } else {
    selectedItem.value = {
      name: newSelectedItem.name,
      selectionType: "FILE",
    };
  }
};

// this is fine since we only render the field when isWriter and selectionMode is FILE
const inputFieldFileName = computed({
  get: () => selectedFileName.value,
  set: (value) => {
    selectedItem.value = {
      name: value,
      selectionType: "FILE",
    };
  },
});

const { clickOutsideExceptions } = useDialogFileExplorerButtons({
  actions: {
    chooseSelectedItem: () => onChooseItem(selectedItem.value?.name ?? ""),
    goIntoSelectedFolder: () => {
      if (selectedItem.value?.selectionType === "FOLDER") {
        changeDirectory(selectedItem.value.name);
      }
      return Promise.resolve();
    },
  },
  selectionMode: toRef(props, "selectionMode"),
  selectedItem,
  isRootParent: computed(() => currentPath.value === null),
});
const openFile = (item: FileExplorerItem) => {
  onChangeSelectedItemIds([item.id]);
  emit("applyAndClose");
};

watch(
  () => backendType.value,
  () => listItems(null, props.initialFilePath).then(handleListItemsResult),
  { immediate: true },
);
</script>

<template>
  <div v-if="isLoading" class="loading-animation">
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
    <div
      v-if="isWriter && props.selectionMode === 'FILE'"
      class="name-input-wrapper"
    >
      <span>Name:</span>
      <InputField v-model="inputFieldFileName" />
    </div>
    <FileExplorer
      class="explorer"
      :is-root-folder="currentPath === null"
      :items="displayedItemsInCurrentFolder"
      :disable-context-menu="true"
      :disable-multi-select="true"
      :disable-dragging="true"
      :click-outside-exceptions="clickOutsideExceptions"
      @change-directory="changeDirectory"
      @open-file="openFile"
      @update:selected-item-ids="onChangeSelectedItemIds"
    />
  </template>
</template>

<style scoped lang="postcss">
.loading-animation {
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
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  flex: 1;
}
</style>
