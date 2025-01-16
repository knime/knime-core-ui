<script setup lang="ts">
import { computed, onMounted, watch } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import type { FileChooserOptions } from "@/nodeDialog/types/FileChooserUiSchema";
import { useFlowSettings } from "../../../composables/components/useFlowVariables";
import FileBrowserButton from "../FileBrowserButton.vue";
import { useFileChooserFileSystemsOptions } from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
import useSideDrawerContent from "../composables/useSideDrawerContent";
import { type FileChooserValue } from "../types/FileChooserProps";

import FSLocationTextControl from "./FSLocationTextControl.vue";
import SideDrawerContent from "./SideDrawerContent.vue";

const props = defineProps<
  VueControlPropsForLabelContent<{
    path: FileChooserValue;
  }>
>();

const isDisabled = computed(
  () =>
    props.disabled ||
    props.control.uischema.options?.fileSystemConnectionMissing,
);

const browseOptions = computed(() => {
  return props.control.uischema.options as FileChooserOptions;
});

const { validCategories } = useFileChooserFileSystemsOptions(browseOptions);
const getDefaultData = () => {
  return {
    path: "",
    timeout: 10000,
    fsCategory: validCategories.value[0],
    context: {
      fsToString: "",
      fsSpecifier: browseOptions.value.fileSystemSpecifier,
    },
  };
};

const data = computed(() => {
  return props.control.data?.path ?? getDefaultData();
});

const onChangePath = (value: FileChooserValue) =>
  props.changeValue({ path: value });

const { flowSettings } = useFlowSettings({
  path: computed(() => props.control.path),
});

const isOverwritten = computed(() =>
  Boolean(flowSettings.value?.controllingFlowVariableName),
);

/**
 * Reset to default data when flow variable is cleared
 */
watch(
  () => isOverwritten.value,
  (value) => {
    if (!value) {
      onChangePath(getDefaultData());
    }
  },
);

const { onFsCategoryUpdate } = useFileChooserStateChange(
  computed(() => props.control.data?.path),
  onChangePath,
  browseOptions,
);

/**
 * This currently can happen in case a node implementation sets the default value to one that is not supported in this frontend.
 * Or when there was a file system connected/removed since the last time the settings were saved.
 * In this case, we switch to a default.
 */
onMounted(() => {
  if (
    !isOverwritten.value &&
    !validCategories.value.includes(props.control.data?.path.fsCategory)
  ) {
    onFsCategoryUpdate(validCategories.value[0]);
  }
});

const { onApply, sideDrawerValue } = useSideDrawerContent<FileChooserValue>({
  onChange: onChangePath,
  initialValue: data,
});
</script>

<template>
  <div class="flex-row">
    <FSLocationTextControl
      :id="labelForId"
      class="flex-grow"
      :model-value="data"
      :disabled="isDisabled"
      :is-local="browseOptions.isLocal"
      :port-index="browseOptions.portIndex"
      :file-system-specifier="browseOptions.fileSystemSpecifier"
      @update:model-value="onChangePath"
    />
    <FileBrowserButton :disabled="isDisabled" @apply="onApply">
      <SideDrawerContent
        :id="labelForId ?? null"
        v-model="sideDrawerValue"
        :disabled="isDisabled"
        :options="browseOptions"
      />
    </FileBrowserButton>
  </div>
</template>

<style lang="postcss" scoped>
.flex-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;

  & .flex-grow {
    flex-grow: 1;
  }

  & .fit-content {
    height: fit-content;
  }
}
</style>
