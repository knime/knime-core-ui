<script setup lang="ts">
import { computed, onMounted, ref, watchEffect } from "vue";

import {
  Breadcrumb,
  type BreadcrumbItem,
  FileExplorer,
  type FileExplorerItem,
  LoadingIcon,
} from "@knime/components";
import CatalogIcon from "@knime/styles/img/icons/db-database.svg";
import SchemaIcon from "@knime/styles/img/icons/db-schema.svg";
import TableLenseIcon from "@knime/styles/img/icons/db-table-view.svg";
import TableIcon from "@knime/styles/img/icons/db-table.svg";
import HouseIcon from "@knime/styles/img/icons/house.svg";

import { useApplyButton } from "../fileChooser/settingsSubPanel";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../fileChooser/settingsSubPanel/SettingsSubPanelForFileChooser.vue";

import {
  type DBItemType,
  type DBTableMetadata,
  type ListItemsResult,
  useDbTableChooserBackend,
} from "./useDbTableChooserBackend";

type FileExplorerItemWithMeta = FileExplorerItem & {
  meta: {
    type: DBItemType;
    tableMetadata: DBTableMetadata | null;
  };
};

const props = withDefaults(
  defineProps<{
    initialPathParts: (string | null)[];
    initialTable?: string | null;
  }>(),
  {
    initialTable: null,
  },
);

/**
 * We use this to make sure that no error messages appear once the user has
 * changed the folder in the file explorer. They should only be visible
 * in the initial view.
 */
const newFolderHasBeenOpened = ref(false);

const { listItems } = useDbTableChooserBackend();

const currentDataIsLoading = ref(true);
const currentData = ref<ListItemsResult | null>(null);

const lastRequestId = ref(0);
const loadNewFolderContent = async (newPathParts: (string | null)[]) => {
  currentDataIsLoading.value = true;
  const thisRequestId = ++lastRequestId.value;

  const listItemsResult = await listItems(
    newPathParts,
    newFolderHasBeenOpened.value ? null : props.initialTable,
  );

  if (thisRequestId === lastRequestId.value) {
    currentData.value = listItemsResult;
    currentDataIsLoading.value = false;
    newFolderHasBeenOpened.value = true;
  }
};

onMounted(() => {
  loadNewFolderContent(props.initialPathParts);
});

const currentFolderPath = computed<string[] | null>(
  () => currentData.value?.nextValidData?.pathParts ?? null,
);

const selectedTable = ref<FileExplorerItem | null>(null);
const selectedDirectory = ref<FileExplorerItem | null>(null);

const emit = defineEmits<{
  tableSelected: [pathParts: (string | null)[]];
}>();

const pathAsString = computed(() => currentFolderPath.value?.join("/"));

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const currentPath = currentData.value?.nextValidData?.pathParts;
  if (!currentPath) {
    return [];
  }
  const items: BreadcrumbItem[] = currentPath.map((name, index) => ({
    text: name,
    path: currentPath.slice(0, index + 1).join("/"),
    clickable: true,
  }));
  items.unshift({
    text: "",
    path: null,
    icon: HouseIcon,
    clickable: true,
  });
  return items;
});

const itemsToDisplay = computed<FileExplorerItem[] | null>(() => {
  if (!currentData.value?.nextValidData) {
    return null;
  }
  const data = currentData.value.nextValidData;
  return data.children.map((item) => ({
    id: [...data.pathParts, item.name].join("/"),
    name: item.name,
    isOpen: false,
    isDirectory: item.type !== "TABLE",
    isOpenableFile: item.type === "TABLE",
    canBeRenamed: false,
    canBeDeleted: false,
    meta: {
      type: item.type,
      tableMetadata: item.type === "TABLE" ? item.tableMetadata : null,
    },
  }));
});

const getTablePath = (table: FileExplorerItem) => {
  const pathPartsLength =
    currentData.value?.nextValidData?.pathParts.length ?? 0;
  const tableMetadata = (table as FileExplorerItemWithMeta).meta.tableMetadata!;
  if (pathPartsLength === 1) {
    return [tableMetadata.containingSchema, table.name];
  }
  if (pathPartsLength === 2) {
    return [
      tableMetadata.containingCatalogue,
      tableMetadata.containingSchema,
      table.name,
    ];
  }
  throw new Error(`Unsupported path length: ${pathPartsLength}`);
};

const onFileOpened = (file: FileExplorerItem) => {
  emit("tableSelected", getTablePath(file));
};

const onDirectoryChanged = (newPathId: string) => {
  selectedDirectory.value = null;
  return loadNewFolderContent(
    newPathId === ".."
      ? [...currentData.value!.nextValidData!.pathParts!, ".."]
      : newPathId.split("/"),
  );
};

const onBreadcrumbItemClick = (item: BreadcrumbItem) => {
  loadNewFolderContent(
    typeof item.path === "string" ? item.path.split("/") : [],
  );
};

const itemIconRenderer = (item: FileExplorerItem) => {
  const type = (item as FileExplorerItemWithMeta).meta.type;
  if (type === "TABLE") {
    const tableMetadata = (item as FileExplorerItemWithMeta).meta
      .tableMetadata!;
    return tableMetadata.tableType === "TABLE" ? TableIcon : TableLenseIcon;
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

onMounted(() => {
  applyButtonDisabled.value = true;
  goIntoFolderButtonDisabled.value = true;
});

const clickOutsideExceptions = [goIntoFolderButtonRef, applyButtonRef];

watchEffect(() => {
  goIntoFolderButtonDisabled.value = selectedDirectory.value === null;
  goIntoFolderButtonText.value =
    selectedDirectory.value === null
      ? "Go into"
      : `Go into ${(
          selectedDirectory.value.meta!.type as string
        ).toLowerCase()}`;
});

watchEffect(() => {
  applyButtonDisabled.value =
    selectedTable.value === null || !selectedTable.value.isOpenableFile;
});

const selectedItemChanged = (selectedItemIds: string[]) => {
  // selectedItemIds will have length 1 or 0 (no multiselection)
  if (selectedItemIds.length === 0) {
    selectedTable.value = null;
    selectedDirectory.value = null;
  } else {
    const item = itemsToDisplay.value!.find(
      (item) => item.id === selectedItemIds[0],
    )!;
    if (item?.isDirectory) {
      selectedDirectory.value = item;
      selectedTable.value = null;
    } else {
      selectedTable.value = item;
      selectedDirectory.value = null;
    }
  }
};

goIntoSelectedFolder.value = () => {
  if (!selectedDirectory.value || !selectedDirectory.value.isDirectory) {
    return Promise.reject(new Error("Selected item is not a directory"));
  }

  const selectedItemName = selectedDirectory.value.name!;
  selectedDirectory.value = null;

  return loadNewFolderContent([
    ...(currentData.value?.nextValidData?.pathParts ?? ""),
    selectedItemName!,
  ]);
};

onApply.value = () => {
  if (!selectedTable.value || !selectedTable.value.isOpenableFile) {
    return Promise.reject(new Error("Selected item is not a table"));
  }

  onFileOpened(selectedTable.value);
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
      <span v-if="currentData?.errorMessage" class="error"
        >({{ currentData.errorMessage }})</span
      >
    </div>
    <div v-if="currentDataIsLoading" class="loading-animation">
      <LoadingIcon class="icon" />
    </div>
    <FileExplorer
      v-if="itemsToDisplay !== null && !currentDataIsLoading"
      class="explorer"
      :full-path="pathAsString"
      :is-root-folder="currentData?.nextValidData?.pathParts.length === 0"
      :items="itemsToDisplay"
      :click-outside-exceptions="clickOutsideExceptions"
      disable-context-menu
      disable-dragging
      disable-multi-select
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
    color: var(--knime-masala);
  }
}

.explorer {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  flex: 1;
}
</style>
