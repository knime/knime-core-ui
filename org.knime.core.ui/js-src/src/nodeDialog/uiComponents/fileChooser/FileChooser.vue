<script setup lang="ts">
import { KnimeService } from "@knime/ui-extension-service";
import { inject, onMounted, ref, type Ref } from "vue";
import FileExplorer from "webapps-common/ui/components/FileExplorer/FileExplorer.vue";
import type { FileExplorerItem } from "webapps-common/ui/components/FileExplorer/types";
import useFileChooserBackend from "./useFileChooserBackend";
import type { Item } from "./useFileChooserBackend";

const toFileExplorerItem = (props: Item): FileExplorerItem => {
  const pathSegments = props.path.split("/");
  const name = pathSegments[pathSegments.length - 1];
  return {
    ...props,
    id: props.path,
    name: name === "" ? "/" : name,
    isOpen: false,
    isOpenableFile: true,
    canBeRenamed: false,
    canBeDeleted: false,
  };
};

const items: Ref<FileExplorerItem[]> = ref([]);

const currentPath: Ref<string | null> = ref(null);

const pathStack: Ref<string[]> = ref([]);

const setNextItems = (nextItems: Item[]) => {
  items.value = nextItems.map(toFileExplorerItem);
};

const { listItems, getRootItems } = useFileChooserBackend(
  inject<() => KnimeService>("getKnimeService")!(),
);

const getItems = () => {
  if (currentPath.value) {
    return listItems(currentPath.value);
  } else {
    return getRootItems();
  }
};

const updateItems = () => {
  getItems().then(setNextItems);
};

onMounted(() => {
  updateItems();
});

const emit = defineEmits(["chooseFile"]);

const changeDirectory = (pathId: string) => {
  if (pathId === ".." && currentPath.value !== "") {
    currentPath.value = pathStack.value.pop() || null;
  } else {
    if (currentPath.value) {
      pathStack.value.push(currentPath.value);
    }
    currentPath.value = pathId;
  }
  updateItems();
};

const openFile = (item: FileExplorerItem) => {
  emit("chooseFile", item.id);
};
</script>

<template>
  {{ currentPath }}
  <FileExplorer
    :is-root-folder="currentPath === null"
    :items="items"
    :disable-context-menu="true"
    @change-directory="changeDirectory"
    @open-file="openFile"
  />
</template>
