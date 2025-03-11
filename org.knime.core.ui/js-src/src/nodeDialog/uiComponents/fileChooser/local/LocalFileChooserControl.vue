<script setup lang="ts">
import { computed } from "vue";

import { InputField } from "@knime/components";
import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import { type FileChooserOptions } from "../../../types/FileChooserUiSchema";
import DialogFileExplorer from "../DialogFileExplorer.vue";
import FileBrowserButton from "../FileBrowserButton.vue";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useSideDrawerContent from "../composables/useSideDrawerContent";

const props = defineProps<VueControlPropsForLabelContent<string>>();

const uiSchemaOptions = computed(
  () =>
    props.control.uischema.options as FileChooserOptions & {
      placeholder?: string;
    },
);
const { sideDrawerValue, updateSideDrawerValue, onApply } =
  useSideDrawerContent<string>({
    onChange: props.changeValue,
    initialValue: computed(() => props.control.data),
  });
const { appendedExtension, filteredExtensions, isLoaded, isWriter } =
  useFileChooserBrowseOptions(uiSchemaOptions);
</script>

<template>
  <div class="flex-row">
    <InputField
      :id="labelForId"
      class="flex-grow"
      :disabled="disabled"
      :model-value="control.data"
      :placeholder="uiSchemaOptions.placeholder"
      :is-valid
      compact
      @update:model-value="changeValue"
    />
    <FileBrowserButton :disabled="disabled" @apply="onApply">
      <DialogFileExplorer
        v-if="isLoaded"
        :backend-type="'local'"
        :disabled="disabled"
        :options="uiSchemaOptions"
        :initial-value="control.data"
        :is-writer="isWriter"
        :filtered-extensions="filteredExtensions"
        :appended-extension="appendedExtension"
        :initial-file-path="sideDrawerValue"
        @choose-file="updateSideDrawerValue"
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
}
</style>
