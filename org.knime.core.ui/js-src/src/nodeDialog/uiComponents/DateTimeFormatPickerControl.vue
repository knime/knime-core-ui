<script setup lang="ts">
import { computed } from "vue";
import { rendererProps } from "@jsonforms/vue";

import {
  DateTimeFormatInput,
  type FormatDateType,
  type FormatWithExample,
} from "@knime/components";

import useDialogControl from "../composables/components/useDialogControl";
import useProvidedState from "../composables/components/useProvidedState";

import LabeledControl from "./label/LabeledControl.vue";

const props = defineProps(rendererProps());

const { control, disabled, onChange } = useDialogControl<string>({
  props,
});

const options = computed(() => {
  return control.value.uischema.options;
});

const allowedFormats = computed<FormatDateType[]>(() => {
  return options.value?.allowedFormats;
});

// TODO(UIEXT-148) implement this function to validate the format.
const validateFormat = (_format: string) => {
  return true;
};

const allFormats = useProvidedState<FormatWithExample[]>(
  computed(() => options.value?.formatProvider),
  [],
);
</script>

<template>
  <LabeledControl
    #default="{ labelForId }"
    :control="control"
    @controlling-flow-variable-set="onChange"
  >
    <DateTimeFormatInput
      :id="labelForId"
      compact
      :disabled="disabled"
      :model-value="control.data"
      :allowed-formats="allowedFormats"
      :all-default-formats="allFormats"
      :format-validator="validateFormat"
      @update:model-value="onChange"
    />
  </LabeledControl>
</template>
