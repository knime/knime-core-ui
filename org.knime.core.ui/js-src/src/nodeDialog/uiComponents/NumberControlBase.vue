<script setup lang="ts">
import { type PropType, computed } from "vue";
import { rendererProps } from "@jsonforms/vue";

import { NumberInput } from "@knime/components";

import useDialogControl from "../composables/components/useDialogControl";
import useProvidedState from "../composables/components/useProvidedState";

import LabeledControl from "./label/LabeledControl.vue";

const props = defineProps({
  ...rendererProps(),
  type: {
    type: String as PropType<"integer" | "double">,
    required: false,
    default: "double",
  },
});
const { control, onChange, disabled } = useDialogControl<number>({ props });

const providedMin = useProvidedState<number | null>(
  control.value.uischema.options?.minProvider,
  null,
);

const providedMax = useProvidedState<number | null>(
  control.value.uischema.options?.maxProvider,
  null,
);

const min = computed(
  () => providedMin.value ?? control.value.uischema.options?.min,
);
const max = computed(
  () => providedMax.value ?? control.value.uischema.options?.max,
);

const onFocusOut = () => {
  const num = control.value.data;
  if (typeof min.value === "number" && num < min.value) {
    onChange(min.value);
  } else if (typeof max.value === "number" && num > max.value) {
    onChange(max.value);
  }
};
</script>

<template>
  <LabeledControl
    #default="{ labelForId }"
    :control="control"
    @controlling-flow-variable-set="onChange"
  >
    <NumberInput
      :id="labelForId ?? undefined"
      class="number-input"
      :disabled="disabled"
      :model-value="control.data"
      :type="type"
      :min="min"
      :max="max"
      compact
      @update:model-value="onChange"
      @focusout="onFocusOut"
    />
  </LabeledControl>
</template>
