<script setup lang="ts">
import { KnimeService } from "@knime/ui-extension-service";
import {
  inject,
  onMounted,
  ref,
  type FunctionalComponent,
  type Ref,
  type SVGAttributes,
} from "vue";
import FileExplorer from "webapps-common/ui/components/FileExplorer/FileExplorer.vue";
import type { FileExplorerItem } from "webapps-common/ui/components/FileExplorer/types";
import useFileChooserBackend, { Entity } from "./useFileChooserBackend";
import type { Item, WorkflowAwareItem } from "./useFileChooserBackend";

import FolderIcon from "webapps-common/ui/assets/img/icons/folder.svg";
import WorkflowIcon from "webapps-common/ui/assets/img/icons/workflow.svg";
import ComponentIcon from "webapps-common/ui/assets/img/icons/node-workflow.svg";
import MetaNodeIcon from "webapps-common/ui/assets/img/icons/workflow-node-stack.svg";

const props = defineProps<{ workflowAware: boolean }>();

const itemIconRenderer = (item: FileExplorerItem) => {
  const typeIcons = {
    "Workflow group": FolderIcon,
    Workflow: WorkflowIcon,
    Component: ComponentIcon,
    Metanode: MetaNodeIcon,
  } as Record<string, FunctionalComponent<SVGAttributes>>;

  return typeIcons[item.meta?.entityType];
};

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

const toFileExplorerItemWorkflowAware = (
  props: WorkflowAwareItem,
): FileExplorerItem => {
  const pathSegments = props.path.split("/");
  const name = pathSegments[pathSegments.length - 1];
  return {
    isDirectory: props.entity === Entity.WORKFLOW_GROUP,
    id: props.path,
    name: name === "" ? "/" : name,
    isOpen: false,
    isOpenableFile: true,
    canBeRenamed: false,
    canBeDeleted: false,
    meta: {
      entityType: props.entity,
    },
  };
};

const items: Ref<FileExplorerItem[]> = ref([]);

const currentPath: Ref<string | null> = ref(null);

const pathStack: Ref<string[]> = ref([]);

const setNextItems = (nextItems: Item[]) => {
  items.value = nextItems.map(toFileExplorerItem);
};

const setNextItemsWorkflowAware = (nextItems: WorkflowAwareItem[]) => {
  items.value = nextItems.map(toFileExplorerItemWorkflowAware);
};

const {
  listItems,
  getRootItems,
  listItemsWorkflowAware,
  getRootWorkflowAwareItems,
} = useFileChooserBackend(inject<() => KnimeService>("getKnimeService")!());

const getItems = () => {
  if (currentPath.value) {
    return listItems(currentPath.value);
  } else {
    return getRootItems();
  }
};

const getItemsWorkflowAware = () => {
  if (currentPath.value) {
    return listItemsWorkflowAware(currentPath.value);
  } else {
    return getRootWorkflowAwareItems();
  }
};

const updateItems = () => {
  if (props.workflowAware) {
    getItemsWorkflowAware().then(setNextItemsWorkflowAware);
  } else {
    getItems().then(setNextItems);
  }
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
    :item-icon-renderer="workflowAware ? itemIconRenderer : null"
    @change-directory="changeDirectory"
    @open-file="openFile"
  />
</template>
