<script setup lang="ts">
import { computed, ref } from "vue";
import { TimeInput } from "./TimeInput.vue";
import useDialogControl from "../composables/components/useDialogControl";
import LabeledControl from "./label/LabeledControl.vue";
import { rendererProps } from "@jsonforms/vue";
import { parse } from "date-fns";
const props = defineProps(rendererProps());
const { control, disabled, onChange } = useDialogControl<string>({ props });

const options = computed(() => control.value.uischema.options);
const minimum = computed(() =>
  options.value?.minimum ? new Date(options.value.minimum) : null,
);
const maximum = computed(() =>
  options.value?.maximum ? new Date(options.value.maximum) : null,
);

console.log("controlData ", control.value.data);
const theDate = Date.parse(control.value.data);
console.log("theDate ", theDate);

const safelyParseTimeWithIncreasingResolutionUntilItFits = (
  timeString: string,
): Date => {
  const timeFormats = [
    "HH:mm:ss.SSSSSSSSS",
    "HH:mm:ss.SSSSSSSS",
    "HH:mm:ss.SSSSSSS",
    "HH:mm:ss.SSSSSS",
    "HH:mm:ss.SSSSS",
    "HH:mm:ss.SSSS",
    "HH:mm:ss.SSS",
    "HH:mm:ss.SS",
    "HH:mm:ss.S",
    "HH:mm:ss",
    "HH:mm",
  ];

  // Check if the time string looks like an ISO date and convert if necessary
  if (new Date(timeString).toString() !== "Invalid Date") {
    return new Date(timeString); // Return ISO-like string directly
  }

  for (const timeFormat of timeFormats) {
    try {
      const parsedTime = parse(timeString, timeFormat, new Date());
      if (!isNaN(parsedTime.getTime())) {
        return parsedTime;
      }
    } catch (error) {
      // Catch parsing errors and skip to the next format
      continue;
    }
  }
  console.error("ERROR: Could not parse time string", timeString);
  return new Date(); // Return current date as fallback
};

const parsedTime = computed(() => {
  return safelyParseTimeWithIncreasingResolutionUntilItFits(control.value.data);
});
console.log("parsedssDate", parsedTime);
</script>

<template>
  <LabeledControl
    #default="{ labelForId }"
    :control="control"
    @controlling-flow-variable-set="onChange"
  >
    <!-- @vue-ignore -->
    <TimeInput
      :id="labelForId"
      two-lines
      :model-value="
        safelyParseTimeWithIncreasingResolutionUntilItFits(control.data)
      "
      class="date-time"
      :required="true"
      compact
      :disabled="disabled"
      @update:model-value="onChange"
    />
  </LabeledControl>
</template>
