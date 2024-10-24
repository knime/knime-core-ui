<script setup lang="ts">
import TimeInput , {onlyLocalTimeFromString} from "./TimeInput.vue";

import useDialogControl from "../composables/components/useDialogControl";
import LabeledControl from "./label/LabeledControl.vue";
import {rendererProps} from "@jsonforms/vue";
import {OnlyLocalTime, onlyLocalTimeToString} from "@/nodeDialog/uiComponents/TimeInput.vue";
import {ref, watch} from "vue";


const props = defineProps(rendererProps());
const { control, disabled, onChange } = useDialogControl<string>({ props });


const onChange2 = (value: OnlyLocalTime) => {
  console.log("onChange2 ",value)
  onChange(onlyLocalTimeToString( value));
};

const model = ref(onlyLocalTimeFromString(control.value.data));
watch(
  () => model.value,
  (newValue : OnlyLocalTime) => {
   onChange2(newValue);
  },
  { deep: true },
)
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
      v-model="
       model
      "
      class="date-time"
      :required="true"
      compact
      :disabled="disabled"
    />
  </LabeledControl>
</template>
