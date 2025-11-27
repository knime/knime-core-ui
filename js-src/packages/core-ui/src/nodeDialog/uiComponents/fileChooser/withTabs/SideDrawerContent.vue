<script setup lang="ts">
import { type Component, computed, ref, toRef } from "vue";

import { TabBar } from "@knime/components";
import ComputerDesktopIcon from "@knime/styles/img/icons/computer-desktop.svg";
import FolderIcon from "@knime/styles/img/icons/folder.svg";
import LinkIcon from "@knime/styles/img/icons/link.svg";
import LocalSpaceIcon from "@knime/styles/img/icons/local-space.svg";
import PluginInputIcon from "@knime/styles/img/icons/plugin-input.svg";
import WorkflowIcon from "@knime/styles/img/icons/workflow.svg";

import DialogFileExplorer from "../DialogFileExplorer.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
import { type BackendType } from "../types";
import type {
  FileChooserProps,
  FileChooserValue,
} from "../types/FileChooserProps";

import ConnectionPreventsTab from "./ConnectionPreventsTab.vue";
import KnimeIcon from "./knime.svg";
import UrlTab from "./url/UrlTab.vue";

const props = defineProps<FileChooserProps>();
const emit = defineEmits(["update:modelValue", "applyAndClose"]);

const uischema = toRef(props, "uischema");
const { onFsCategoryUpdate, onPathUpdate, onTimeoutUpdate } =
  useFileChooserStateChange(
    toRef(props, "modelValue"),
    (value: FileChooserValue) => {
      emit("update:modelValue", value);
    },
    uischema,
  );

const selectionMode = computed(
  () => uischema.value.options?.selectionMode ?? "FILE",
);
const {
  filteredExtensions,
  appendedExtension,
  isWriter,
  isLocal,
  isLoaded,
  spacePath,
  mountId,
  relativeWorkflowPath,
  isConnected,
  portFileSystemName,
  portIndex,
  fileSystems,
} = useFileChooserBrowseOptions(uischema);

type TabType = "CONNECTED" | "LOCAL" | "SPACE" | "EMBEDDED" | "CUSTOM_URL";

export type TabSpec = {
  value: TabType;
  label: string;
  icon: Component;
};

const conditionalTab = (condition: boolean | undefined, tab: TabSpec) =>
  condition ? [tab] : [];

const possibleCategories = computed<TabSpec[]>(() => {
  return [
    ...conditionalTab(isConnected.value, {
      value: "CONNECTED",
      label: portFileSystemName.value,
      icon: PluginInputIcon,
    }),
    ...conditionalTab(isLocal.value, {
      value: "LOCAL",
      label: "Local file system",
      icon: ComputerDesktopIcon,
    }),
    ...conditionalTab(fileSystems.value.includes("SPACE"), {
      value: "SPACE",
      label: mountId.value,
      icon: isLocal.value ? LocalSpaceIcon : KnimeIcon,
    }),
    ...conditionalTab(fileSystems.value.includes("EMBEDDED"), {
      value: "EMBEDDED",
      label: "Embedded data",
      icon: FolderIcon,
    }),
    ...conditionalTab(fileSystems.value.includes("CUSTOM_URL"), {
      value: "CUSTOM_URL",
      label: "URL",
      icon: LinkIcon,
    }),
  ];
});

const isRelativeToWorkflow = computed(
  () => props.modelValue.fsCategory === "relative-to-workflow",
);
const initialFSCategoryIsRelativeToWorkflow = isRelativeToWorkflow.value;
const relativeToWorkflowLastSelected = ref(isRelativeToWorkflow.value);
const updateIsRelativeTo = (isRelative: boolean) => {
  relativeToWorkflowLastSelected.value = isRelative;
  onFsCategoryUpdate(
    isRelative ? "relative-to-workflow" : "relative-to-current-hubspace",
  );
};

const tabType = computed<TabType>({
  get: () => {
    const fsCategory = props.modelValue.fsCategory;
    if (
      fsCategory === "relative-to-workflow" ||
      fsCategory === "relative-to-current-hubspace"
    ) {
      return "SPACE";
    }
    if (fsCategory === "relative-to-embedded-data") {
      return "EMBEDDED";
    }
    return fsCategory;
  },
  set: (value: TabType) => {
    if (value === "SPACE") {
      onFsCategoryUpdate(
        relativeToWorkflowLastSelected.value ||
          initialFSCategoryIsRelativeToWorkflow
          ? "relative-to-workflow"
          : "relative-to-current-hubspace",
      );
    } else if (value === "EMBEDDED") {
      onFsCategoryUpdate("relative-to-embedded-data");
    } else {
      onFsCategoryUpdate(value);
    }
  },
});

const relativeToOptions = computed(() => {
  const isSpaceTab = tabType.value === "SPACE";
  const relativeWorkflowPathValue = relativeWorkflowPath.value;
  return isSpaceTab && relativeWorkflowPathValue
    ? {
        relativeToPath: relativeWorkflowPathValue,
        rootLabel: "Space",
        relativeRootLabel: "Workflow",
        isRelativeTo: isRelativeToWorkflow.value,
        relativeRootIcon: WorkflowIcon,
        initialFilePathIsRelative: initialFSCategoryIsRelativeToWorkflow,
      }
    : null;
});

const backendType = computed<BackendType>(() =>
  getBackendType(props.modelValue.fsCategory, portIndex.value),
);

const breadcrumbRoot = computed(() => {
  if (props.modelValue.fsCategory === "relative-to-current-hubspace") {
    return spacePath.value;
  }
  if (props.modelValue.fsCategory === "relative-to-embedded-data") {
    return "Data";
  }
  return null;
});

const browseAction: Record<Exclude<TabType, "CONNECTED">, string> = {
  SPACE: "browse the current space",
  EMBEDDED: "browse the embedded data",
  CUSTOM_URL: "use a URL to read files",
  LOCAL: "browse the local file system",
};
</script>

<template>
  <div class="flex">
    <TabBar v-model="tabType" :possible-values="possibleCategories" />
    <div class="flex-grow">
      <ConnectionPreventsTab
        v-if="isConnected && modelValue.fsCategory !== 'CONNECTED'"
        :browse-action="browseAction[modelValue.fsCategory]"
      />
      <UrlTab
        v-else-if="modelValue.fsCategory === 'CUSTOM_URL'"
        :id="id"
        :model-value="modelValue"
        :disabled="disabled"
        @update:path="onPathUpdate"
        @update:timeout="onTimeoutUpdate"
      />
      <DialogFileExplorer
        v-else-if="isLoaded"
        :id="id"
        :filtered-extensions="filteredExtensions"
        :appended-extension="appendedExtension"
        :is-writer="isWriter"
        :backend-type="backendType"
        :initial-file-path="modelValue.path"
        :breadcrumb-root="breadcrumbRoot"
        :selection-mode="selectionMode"
        :relative-to-options="relativeToOptions"
        @choose-item="onPathUpdate"
        @update:is-relative-to="updateIsRelativeTo"
        @apply-and-close="emit('applyAndClose')"
      />
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.flex {
  display: flex;
  flex-direction: column;
  height: 100%;

  & :deep(.carousel) {
    &::after {
      bottom: 18px;
    }

    & .tab-bar {
      padding-bottom: 16px;

      & > .overflow {
        height: 42px;

        & span {
          font-size: 13px;
          height: 42px;
          line-height: 42px;
        }

        & svg {
          width: 13px;
          height: 13px;
          stroke-width: calc(32px / 13);
          vertical-align: -2px;
        }
      }
    }
  }

  & .flex-grow {
    flex-grow: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
}
</style>
