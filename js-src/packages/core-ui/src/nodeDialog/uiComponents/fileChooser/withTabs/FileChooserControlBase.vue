<script setup lang="ts" generic="T">
import { computed, ref, watch } from "vue";

import {
  type VueControlPropsForLabelContent,
  useProvidedState,
} from "@knime/jsonforms";

import type { FileChooserUiSchema } from "@/nodeDialog/types/FileChooserUiSchema";
import { useFlowSettings } from "../../../composables/components/useFlowVariables";
import FileBrowserButton from "../FileBrowserButton.vue";
import { useFileSystems } from "../composables/useFileChooserBrowseOptions";
import useFileChooserStateChange from "../composables/useFileChooserStateChange";
import useSideDrawerContent from "../composables/useSideDrawerContent";
import { type FileChooserValue } from "../types/FileChooserProps";

import FSLocationTextControl from "./FSLocationTextControl.vue";
import SideDrawerContent from "./SideDrawerContent.vue";
import useFsNotSupported from "./useFsNotSupported";

const props = defineProps<
  VueControlPropsForLabelContent<T> & {
    toPath: (t: T) => FileChooserValue;
    changePath: (path: FileChooserValue) => void;
  }
>();

const uischema = computed(() => props.control.uischema as FileChooserUiSchema);

const connectedFSOptions = useProvidedState(uischema, "connectedFSOptions");
// Merge connectedFSOptions from state provider into options for child components
const options = computed(() => ({
  ...uischema.value.options!,
  connectedFSOptions:
    connectedFSOptions.value ?? uischema.value.options?.connectedFSOptions,
}));
const {
  validCategories,
  isConnectedButNoFileConnectionIsAvailable,
  isConnected,
} = useFileSystems(options);

const getDefaultData = () => {
  return {
    path: "",
    timeout: 10000,
    fsCategory: validCategories.value[0],
    context: {
      fsToString: "",
      fsSpecifier: connectedFSOptions.value?.fileSystemSpecifier,
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

const textControl = ref<InstanceType<typeof FSLocationTextControl> | null>(
  null,
);

const { fsNotSupportedComponent, fsNotSupported } = useFsNotSupported({
  data,
  validCategories,
  isOverwritten,
  isConnected,
  onFsCategoryUpdate,
  clearPath: () => {
    props.changePath(getDefaultData());
    setTimeout(() => textControl.value?.focusInput?.(), 0);
  },
});

const { onApply, sideDrawerValue } = useSideDrawerContent<FileChooserValue>({
  onChange: props.changePath,
  initialValue: data,
});

const isDisabled = computed(
  () =>
    props.disabled ||
    isConnectedButNoFileConnectionIsAvailable.value ||
    validCategories.value.length === 0 ||
    fsNotSupported.value,
);
</script>

<template>
  <div class="flex-row">
    <FSLocationTextControl
      :id="labelForId"
      ref="textControl"
      class="flex-grow"
      :model-value="data"
      :disabled="isDisabled"
      :is-valid
      :options
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
        @apply-and-close="applyAndClose"
      />
    </FileBrowserButton>
  </div>
  <component :is="fsNotSupportedComponent" />
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
