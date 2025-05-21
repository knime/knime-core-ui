<script setup lang="ts">
import { computed, ref } from "vue";
import { computedAsync } from "@vueuse/core";

import {
  Breadcrumb,
  type BreadcrumbItem,
  FileExplorer,
  type FileExplorerItem,
  LoadingIcon,
} from "@knime/components";
import CatalogIcon from "@knime/styles/img/icons/db-database.svg";
import SchemaIcon from "@knime/styles/img/icons/db-schema.svg";
import TableIcon from "@knime/styles/img/icons/db-table.svg";
import HouseIcon from "@knime/styles/img/icons/house.svg";

import { useApplyButton } from "../fileChooser/settingsSubPanel";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../fileChooser/settingsSubPanel/SettingsSubPanelForFileChooser.vue";

import {
  type DBItemType,
  type ListItemsResult,
  useDbTableChooserBackend,
} from "./useDbTableChooserBackend";

type FileExplorerItemWithMeta = FileExplorerItem & {
  meta: {
    type: DBItemType;
  };
};

const { listItems } = useDbTableChooserBackend();

const currentFolderPath = defineModel<string[]>("path", {
  default: () => [],
});

const selectedItem = ref<FileExplorerItem | null>(null);

const emit = defineEmits<{
  tableSelected: [pathParts: string[]];
}>();

const pathAsString = computed({
  get: () => currentFolderPath.value.join("/"),
  set: (value: string) => {
    if (value === "") {
      currentFolderPath.value = [];
    } else if (value === "..") {
      currentFolderPath.value = currentFolderPath.value.slice(0, -1);
    } else {
      currentFolderPath.value = value.split("/");
    }
  },
});

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = currentFolderPath.value.map(
    (name, index) => ({
      text: name,
      path: currentFolderPath.value.slice(0, index + 1).join("/"),
      clickable: true,
    }),
  );

  items.unshift({
    text: "",
    path: "",
    icon: HouseIcon,
    clickable: true,
  });

  return items;
});

const currentDataIsLoading = ref(false);
const currentData = computedAsync<ListItemsResult>(
  () => listItems(currentFolderPath.value),
  {
    type: "SUCCESS",
    data: {
      path: [],
      children: [],
    },
  },
  currentDataIsLoading,
);

const itemsToDisplay = computed<FileExplorerItem[]>(() => {
  if (currentData.value.type === "ERROR") {
    return [];
  }

  return currentData.value.data.children.map((item) => ({
    id: [...currentFolderPath.value, item.name].join("/"),
    name: item.name,
    isOpen: false,
    isDirectory: item.type !== "TABLE",
    isOpenableFile: item.type === "TABLE",
    canBeRenamed: false,
    canBeDeleted: false,
    meta: {
      type: item.type,
    },
  }));
});

const onFileOpened = (file: FileExplorerItem) => {
  emit("tableSelected", [...currentFolderPath.value, file.name]);
};

const onDirectoryChanged = (newPathId: string) =>
  (pathAsString.value = newPathId);

const onBreadcrumbItemClick = (item: BreadcrumbItem) => {
  pathAsString.value = item.path ?? "";
};

const itemIconRenderer = (item: FileExplorerItem) => {
  const type = (item as FileExplorerItemWithMeta).meta?.type;
  if (type === "TABLE") {
    return TableIcon;
  } else if (type === "SCHEMA") {
    return SchemaIcon;
  } else if (type === "CATALOG") {
    return CatalogIcon;
  }

  throw new Error(`Unknown item type "${type}" for item "${item.name}"`);
};

const {
  onApply: goIntoSelectedFolder,
  text: goIntoFolderButtonText,
  disabled: goIntoFolderButtonDisabled,
  element: goIntoFolderButtonRef,
} = useApplyButton(GO_INTO_FOLDER_INJECTION_KEY);

const {
  onApply,
  disabled: applyButtonDisabled,
  element: applyButtonRef,
} = useApplyButton();

const clickOutsideExceptions = [goIntoFolderButtonRef, applyButtonRef];

const selectedItemChanged = (selectedItemIds: string[]) => {
  // selectedItemIds will have length 1 or 0 (no multiselection)
  goIntoFolderButtonText.value = "Go into";
  if (selectedItemIds.length === 0) {
    goIntoFolderButtonDisabled.value = true;
    selectedItem.value = null;
  } else {
    const item = itemsToDisplay.value.find(
      (item) => item.id === selectedItemIds[0],
    );
    selectedItem.value = item!;

    goIntoFolderButtonDisabled.value = !item?.isDirectory;
    if (item?.isDirectory) {
      goIntoFolderButtonText.value = `Go into ${item.meta?.type.toLowerCase()}`;
    } else {
      goIntoFolderButtonText.value = "Go into";
    }

    applyButtonDisabled.value = !item?.isOpenableFile;
  }
};

goIntoSelectedFolder.value = () => {
  if (!selectedItem.value || !selectedItem.value.isDirectory) {
    return Promise.reject(new Error("Selected item is not a directory"));
  }

  currentFolderPath.value = [
    ...currentFolderPath.value,
    selectedItem.value?.name!,
  ];

  return Promise.resolve();
};

onApply.value = () => {
  if (!selectedItem.value || !selectedItem.value.isOpenableFile) {
    return Promise.reject(new Error("Selected item is not an openable file"));
  }

  const item = itemsToDisplay.value.find(
    (item) => item.id === selectedItem.value?.id,
  );

  onFileOpened(item!);

  return Promise.resolve();
};
</script>

<template>
  <div class="flex-column">
    <div class="current-path">
      <Breadcrumb
        class="breadcrumb"
        :items="breadcrumbItems"
        no-wrap
        compact
        @click-item="onBreadcrumbItemClick as any"
      />
      <span v-if="currentData.type === 'ERROR'" class="error"
        >({{ currentData.message }})</span
      >
    </div>
    <div v-if="currentDataIsLoading" class="loading-animation">
      <LoadingIcon class="icon" />
    </div>
    <FileExplorer
      v-else
      class="explorer"
      :full-path="pathAsString"
      :is-root-folder="currentFolderPath.length === 0"
      :items="itemsToDisplay"
      :click-outside-exception="clickOutsideExceptions"
      emit-apply-and-close
      @open-file="onFileOpened"
      @change-directory="onDirectoryChanged"
      @update:selected-item-ids="selectedItemChanged"
    >
      <template #itemIcon="{ item }">
        <Component :is="itemIconRenderer(item)" />
      </template>
    </FileExplorer>
  </div>
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
    width: 45px;
    height: 45px;
  }
}

.flex-column {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-8);
  height: 100%;
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

.explorer {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  flex: 1;
}
</style>
