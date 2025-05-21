<script setup lang="ts">
import { computed } from "vue";
import { computedAsync } from "@vueuse/core";

import { FileExplorer, type FileExplorerItem } from "@knime/components";

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
  set: (value: string) =>
    (currentFolderPath.value =
      value === ".." ? currentFolderPath.value.slice(0, -1) : value.split("/")),
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

const itemsToDisplay = computed(() => {
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
</script>

<template>
  <div class="flex-column">
    <div class="error-container">
      <template v-if="currentData.type === 'ERROR'">
        {{ currentData.message }}
      </template>
    </div>
    <FileExplorer
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
  gap: var(--space-4);
  margin-top: var(--space-8);
}

.error-container {
  min-height: 20px;
  line-height: 15px;
  font-size: 12px;
  color: var(--knime-coral);
  display: flex;
  align-items: center;
}
</style>
