<script setup lang="ts">
import { computed } from "vue";

import { InputField } from "@knime/components";
import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import { type FileChooserUiSchema } from "../../../types/FileChooserUiSchema";
import DialogFileExplorer from "../DialogFileExplorer.vue";
import FileBrowserButton from "../FileBrowserButton.vue";
import { useFileChooserBrowseOptions } from "../composables/useFileChooserBrowseOptions";
import useSideDrawerContent from "../composables/useSideDrawerContent";
import type { BackendType } from "../types";

const props = defineProps<
  VueControlPropsForLabelContent<string> & { backendType: BackendType }
>();
const uischema = computed(() => props.control.uischema as FileChooserUiSchema);
const { sideDrawerValue, updateSideDrawerValue, onApply } =
  useSideDrawerContent<string>({
    onChange: props.changeValue,
    initialValue: computed(() => props.control.data),
  });
const { appendedExtension, filteredExtensions, isLoaded, isWriter } =
  useFileChooserBrowseOptions(uischema);
const selectionMode = computed(
  () => uischema.value.options?.selectionMode ?? "FILE",
);
const placeholder = computed(() => uischema.value.options?.placeholder);
</script>

<template>
  <div class="flex-row">
    <InputField
      :id="labelForId"
      class="flex-grow"
      :disabled="disabled"
      :model-value="control.data"
      :placeholder
      :is-valid
      compact
      @update:model-value="changeValue"
    />
    <FileBrowserButton
      #default="{ applyAndClose, cancel }"
      :disabled="disabled"
      @apply="onApply"
    >
      <DialogFileExplorer
        v-if="isLoaded"
        :backend-type="backendType"
        :disabled="disabled"
        :initial-value="control.data"
        :is-writer="isWriter"
        :filtered-extensions="filteredExtensions"
        :appended-extension="appendedExtension"
        :initial-file-path="sideDrawerValue"
        :selection-mode="selectionMode"
        @choose-item="updateSideDrawerValue"
        @apply-and-close="applyAndClose"
        @cancel="cancel"
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
