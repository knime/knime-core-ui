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

const {
  control,
  disabled,
  onChange: dialogueControlOnChange,
} = useDialogControl<string>({
  props,
});

const options = computed(() => {
  return control.value.uischema.options;
});

const allowedFormats = computed<FormatDateType[]>(() => {
  return options.value?.allowedFormats;
});

// TODO: take the initial value from the control and put it
// into the recents if it is not already there. For this you
// will need to check its validity and generate an example,
// using backend communication.
const allBaseFormats = useProvidedState<FormatWithExample[] | null>(
  computed(() => options.value?.formatProvider),
  null,
);

const onChange = (newValue: string) => {
  dialogueControlOnChange(newValue);
};

// TODO: Listen to the 'committed' event of the DateTimeFormatInput.
// If the format is not in the list and is valid,
// get an example from the backend, add it to the list of formats.
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
      :all-default-formats="allBaseFormats"
      :is-valid="true"
      @update:model-value="onChange"
    />
  </LabeledControl>
</template>
