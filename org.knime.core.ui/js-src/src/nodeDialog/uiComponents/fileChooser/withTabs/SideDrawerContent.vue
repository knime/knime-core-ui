<script setup lang="ts">
import type FileChooserProps from "../types/FileChooserProps";
import type { FileChooserValue, FSCategory } from "../types/FileChooserProps";
import FileChooser from "../FileChooser.vue";
import CustomUrlFileChooser from "../CustomUrlFileChooser.vue";
import { mergeDeep } from "@/nodeDialog/utils";
import { computed } from "vue";
import InputField from "webapps-common/ui/components/forms/InputField.vue";
import TabBar from "webapps-common/ui/components/TabBar.vue";

const props = defineProps<FileChooserProps>();
const emit = defineEmits(["update:modelValue"]);

const onChange = (value: FileChooserValue) => {
  emit("update:modelValue", value);
};

const onPathUpdate = (path: string) => {
  onChange(mergeDeep(props.modelValue, { path }));
};

const onTimeoutUpdate = (timeout: number) => {
  onChange(mergeDeep(props.modelValue, { timeout }));
};

const onFsCategoryUpdate = (fsCategory: keyof typeof FSCategory) => {
  onChange(mergeDeep(props.modelValue, { fsCategory }));
};

const possibleCategories: { value: keyof typeof FSCategory; label: string }[] =
  [
    {
      value: "relative-to-current-hubspace",
      label: "Playground Hub",
    },
    {
      value: "CUSTOM_URL",
      label: "URL",
    },
  ];

/* const stringFileChooserPlaceholder = computed(() =>
  props.modelValue.fsCategory === "LOCAL"
    ? "Local file path"
    : "Path relative to hub space",
);

const stringFileChooserOptions = computed(() => ({
  placeholder: stringFileChooserPlaceholder.value,
  ...props.browseOptions,
})); */

const isSupported = computed(() =>
  possibleCategories
    .map(({ value }) => value)
    .includes(props.modelValue.fsCategory),
);
</script>

<template>
  <template v-if="isSupported">
    <TabBar
      :possible-values="possibleCategories"
      :model-value="modelValue.fsCategory"
      @update:model-value="onFsCategoryUpdate"
    />
    <CustomUrlFileChooser
      v-if="modelValue.fsCategory === 'CUSTOM_URL'"
      :id="id"
      :model-value="modelValue"
      :disabled="disabled"
      @update:path="onPathUpdate"
      @update:timeout="onTimeoutUpdate"
    />
    <FileChooser
      v-else
      :id="id"
      backend-type="relativeToCurrentHubSpace"
      :initial-file-path="modelValue.path"
    />
  </template>
  <InputField
    v-else
    :id="id"
    :model-value="modelValue.context?.fsToString"
    disabled
  />
</template>
