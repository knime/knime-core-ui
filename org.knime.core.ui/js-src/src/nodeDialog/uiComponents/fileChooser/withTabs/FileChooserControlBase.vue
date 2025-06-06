<script setup lang="ts" generic="T">
import { computed, onMounted, watch } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import type { FileChooserUiSchema } from "@/nodeDialog/types/FileChooserUiSchema";
import { useFlowSettings } from "../../../composables/components/useFlowVariables";
import FileBrowserButton from "../FileBrowserButton.vue";
import { useFileChooserFileSystemsOptions } from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
import useSideDrawerContent from "../composables/useSideDrawerContent";
import { type FileChooserValue } from "../types/FileChooserProps";

import FSLocationTextControl from "./FSLocationTextControl.vue";
import SideDrawerContent from "./SideDrawerContent.vue";

const props = defineProps<
  VueControlPropsForLabelContent<T> & {
    toPath: (t: T) => FileChooserValue;
    changePath: (path: FileChooserValue) => void;
  }
>();

const isDisabled = computed(
  () =>
    props.disabled ||
    props.control.uischema.options?.fileSystemConnectionMissing,
);

const uischema = computed(() => props.control.uischema as FileChooserUiSchema);

const { validCategories } = useFileChooserFileSystemsOptions(uischema);
const getDefaultData = () => {
  return {
    path: "",
    timeout: 10000,
    fsCategory: validCategories.value[0],
    context: {
      fsToString: "",
      fsSpecifier: uischema.value.options?.fileSystemSpecifier,
    },
  };
};

const data = computed(() => {
  if (props.control.data) {
    return props.toPath(props.control.data);
  }
  return getDefaultData();
});

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
      props.changePath(getDefaultData());
    }
  },
);

const { onFsCategoryUpdate } = useFileChooserStateChange(
  data,
  props.changePath,
  uischema,
);

/**
 * This currently can happen in case a node implementation sets the default value to one that is not supported in this frontend.
 * Or when there was a file system connected/removed since the last time the settings were saved.
 * In this case, we switch to a default.
 */
onMounted(() => {
  if (
    !isOverwritten.value &&
    !validCategories.value.includes(data.value.fsCategory)
  ) {
    onFsCategoryUpdate(validCategories.value[0]);
  }
});

const { onApply, sideDrawerValue } = useSideDrawerContent<FileChooserValue>({
  onChange: props.changePath,
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
      :is-local="uischema.options?.isLocal"
      :is-valid
      :port-index="uischema.options?.portIndex"
      :file-system-specifier="uischema.options?.fileSystemSpecifier"
      @update:model-value="changePath"
    />
    <FileBrowserButton
      #default="{ applyAndClose }"
      :disabled="isDisabled"
      @apply="onApply"
    >
      <SideDrawerContent
        :id="labelForId ?? null"
        v-model="sideDrawerValue"
        :disabled="isDisabled"
        :uischema
        :selection-mode="uischema.options?.selectionMode ?? 'FILE'"
        @apply-and-close="applyAndClose"
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
