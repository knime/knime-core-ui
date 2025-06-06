<script setup lang="ts">
import { type FunctionalComponent, computed, toRef } from "vue";

import { TabBar } from "@knime/components";
import ComputerDesktopIcon from "@knime/styles/img/icons/computer-desktop.svg";
import FolderIcon from "@knime/styles/img/icons/folder.svg";
import LinkIcon from "@knime/styles/img/icons/link.svg";
import LocalSpaceIcon from "@knime/styles/img/icons/local-space.svg";
import PluginInputIcon from "@knime/styles/img/icons/plugin-input.svg";

import DialogFileExplorer from "../DialogFileExplorer.vue";
import { getBackendType } from "../composables/useFileChooserBackend";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
import { type BackendType } from "../types";
import type {
  FSCategory,
  FileChooserProps,
  FileChooserValue,
} from "../types/FileChooserProps";

import ConnectionPreventsTab from "./ConnectionPreventsTab.vue";
import KnimeIcon from "./knime.svg";
import UrlTab from "./url/UrlTab.vue";

type PropType = FileChooserProps & {
  selectionMode: "FILE" | "FOLDER";
};

const props = defineProps<PropType>();
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
const {
  filteredExtensions,
  appendedExtension,
  isWriter,
  isLocal,
  isLoaded,
  spacePath,
  mountId,
  isConnected,
  portFileSystemName,
  portIndex,
} = useFileChooserBrowseOptions(uischema);

type TabSpec = {
  value: keyof typeof FSCategory;
  label: string;
  icon: FunctionalComponent;
};

const localFileSystemTab: TabSpec[] = isLocal.value
  ? [
      {
        value: "LOCAL",
        label: "Local File System",
        icon: ComputerDesktopIcon,
      },
    ]
  : [];

const connectedFileSystemTab: TabSpec[] = isConnected.value
  ? [
      {
        value: "CONNECTED",
        label: portFileSystemName.value,
        icon: PluginInputIcon,
      },
    ]
  : [];

const possibleCategories: TabSpec[] = [
  ...connectedFileSystemTab,
  ...localFileSystemTab,
  {
    value: "relative-to-current-hubspace",
    label: mountId.value,
    icon: isLocal.value ? LocalSpaceIcon : KnimeIcon,
  },
  {
    value: "relative-to-embedded-data",
    label: "Embedded Data",
    icon: FolderIcon,
  },
  {
    value: "CUSTOM_URL",
    label: "URL",
    icon: LinkIcon,
  },
];

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

const browseAction: Record<
  Exclude<keyof typeof FSCategory, "CONNECTED">,
  string
> = {
  "relative-to-current-hubspace": "browse the current space",
  "relative-to-embedded-data": "browse the embedded data",
  CUSTOM_URL: "use a URL to read files",
  LOCAL: "browse the local file system",
};
</script>

<template>
  <div class="flex">
    <TabBar
      :possible-values="possibleCategories"
      :model-value="modelValue.fsCategory"
      @update:model-value="onFsCategoryUpdate"
    />
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
        @choose-item="onPathUpdate"
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
