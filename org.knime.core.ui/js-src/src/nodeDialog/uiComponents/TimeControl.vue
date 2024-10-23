<script setup lang="ts">
import {computed, ref} from "vue";
import { DateTimeInput } from "@knime/components/date-time-input";
import useDialogControl from "../composables/components/useDialogControl";
import LabeledControl from "./label/LabeledControl.vue";
import { rendererProps } from "@jsonforms/vue";
import {parse} from "date-fns";
import { zonedTimeToUtc } from "date-fns-tz";
const props = defineProps(rendererProps());
const { control, disabled, onChange } = useDialogControl<string>({ props });

const options = computed(() => control.value.uischema.options);
const minimum = computed(() =>
  options.value?.minimum ? new Date(options.value.minimum) : null,
);
const maximum = computed(() =>
  options.value?.maximum ? new Date(options.value.maximum) : null,
);

console.log("controlData ",control.value.data)
const theDate =  Date.parse(control.value.data)
console.log("theDate ",theDate)


const safelyParseTimeWithIncreasingResolutionUntilItFits = (timeString: string) => {
  const timeFormats = [
    'HH:mm:ss.SSSSSSSSS',
    'HH:mm:ss.SSSSSSSS',
    'HH:mm:ss.SSSSSSS',
    'HH:mm:ss.SSSSSS',
    'HH:mm:ss.SSSSS',
    'HH:mm:ss.SSSS',
    'HH:mm:ss.SSS',
    'HH:mm:ss.SS',
    'HH:mm:ss.S',
    'HH:mm:ss',
    'HH:mm',
  ];

  for (const timeFormat of timeFormats) {
    const parsedTime = parse(timeString, timeFormat, new Date());
    if (!isNaN(parsedTime.getTime())) {
      return parsedTime;
    }
  }
  return new Date().toLocaleTimeString();
};

const parsedTime = ref(safelyParseTimeWithIncreasingResolutionUntilItFits(control.value.data));
console.log("parsedDate",parsedTime)

</script>

<template>
  <LabeledControl
    #default="{ labelForId }"
    :control="control"
    @controlling-flow-variable-set="onChange"
  >
    <!-- @vue-ignore -->
    <DateTimeInput
      :id="labelForId"
      two-lines
      :model-value="control.data"
      class="date-time"
      :required="true"
      :show-time="true"
      :show-seconds="options?.showSeconds"
      :show-milliseconds="props.schema.showAdvancedSettings"
      :show-date="false"
      :timezone="options?.timezone"
      :date-format="options?.dateFormat"
      :min="minimum"
      :max="maximum"
      compact
      :disabled="disabled"
      @update:model-value="onChange"
    />
  </LabeledControl>
</template>
