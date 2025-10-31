<script setup lang="ts">
import { computed } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";
import { useProvidedState } from "@knime/jsonforms";

import {
  type FileSystemOption,
  type StringFileChooserUiSchema,
} from "../../../types/FileChooserUiSchema";
import { getBackendType } from "../composables/useFileChooserBackend";
import { FSCategory } from "../types/FileChooserProps";

import SingleFileSystemFileChooserControl from "./SingleFileSystemFileChooserControl.vue";

const props = defineProps<VueControlPropsForLabelContent<string>>();
const uischema = computed<StringFileChooserUiSchema>(
  () => props.control.uischema as StringFileChooserUiSchema,
);
const fileSystemId = useProvidedState(uischema, "fileSystemId");

const fileSystem = computed(() => uischema.value.options?.fileSystem ?? null);

const mapFileSystemToCategory = (
  fs: FileSystemOption,
): keyof typeof FSCategory => {
  switch (fs) {
    case "LOCAL":
      return "LOCAL";
    case "CONNECTED":
      return "CONNECTED";
    case "SPACE":
      return "relative-to-current-hubspace";
    case "EMBEDDED":
      return "relative-to-embedded-data";
    case "CUSTOM_URL":
      return "CUSTOM_URL";
    default:
      throw new Error(`Unsupported file system: ${fs}`);
  }
};

const backendType = computed(() => {
  if (fileSystem.value === null) {
    return fileSystemId.value;
  }
  const fsCategory = mapFileSystemToCategory(fileSystem.value);
  const portIndex = uischema.value.options?.connectedFSOptions?.portIndex;
  return getBackendType(fsCategory, portIndex);
});

const disabledSinceOnlyConnectedIsAllowed = computed(() => {
  const isConnectedFS = fileSystem.value === "CONNECTED";
  const hasConnectedFSOptions = Boolean(
    uischema.value.options?.connectedFSOptions,
  );
  return isConnectedFS && !hasConnectedFSOptions;
});

const isDisabled = computed(
  () =>
    props.disabled ||
    uischema.value.options?.connectedFSOptions?.fileSystemConnectionMissing ||
    disabledSinceOnlyConnectedIsAllowed.value,
);
</script>

<template>
  <SingleFileSystemFileChooserControl
    v-if="backendType"
    v-bind="$props"
    :disabled="isDisabled"
    :backend-type="backendType"
  />
</template>
