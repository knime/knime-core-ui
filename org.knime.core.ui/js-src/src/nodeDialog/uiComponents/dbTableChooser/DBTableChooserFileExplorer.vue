<script setup lang="ts">
import { computed } from "vue";
import { computedAsync } from "@vueuse/core";

import {
  Breadcrumb,
  type BreadcrumbItem,
  FileExplorer,
  type FileExplorerItem,
} from "@knime/components";
import HouseIcon from "@knime/styles/img/icons/house.svg";

import {
  type ListItemsResult,
  useDbTableChooserBackend,
} from "./useDbTableChooserBackend";

const { listItems } = useDbTableChooserBackend();

const currentFolderPath = defineModel<string[]>("path", {
  default: () => [],
});

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

  if (currentFolderPath.value.length > 0) {
    items.unshift({
      text: "",
      path: "",
      icon: HouseIcon,
      clickable: true,
    });
  }

  return items;
});

const currentData = computedAsync<ListItemsResult>(
  () => listItems(currentFolderPath.value),
  {
    type: "SUCCESS",
    data: {
      path: [],
      children: [],
    },
  },
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
    <FileExplorer
      class="explorer"
      :full-path="pathAsString"
      :is-root-folder="currentFolderPath.length === 0"
      :items="itemsToDisplay"
      @open-file="onFileOpened"
      @change-directory="onDirectoryChanged"
    />
  </div>
</template>

<style scoped lang="postcss">
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
