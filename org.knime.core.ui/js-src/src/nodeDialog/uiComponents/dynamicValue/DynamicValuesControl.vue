<script setup lang="ts">
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps } from "@knime/jsonforms";

import DynamicValueControl, {
  type DynamicValueType,
} from "./DynamicValueControl.vue";

enum InputKind {
  SINGLE = "SINGLE",
  DOUBLE = "DOUBLE",
  COLLECTION = "COLLECTION",
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars, unused-imports/no-unused-vars
export interface DynamicValueInputType {
  values: DynamicValueType[];
  inputKind: InputKind;
}

defineProps<VueControlProps<DynamicValueInputType>>();
</script>

<template>
  <DynamicValueControl
    v-for="(value, index) in control.data.values"
    #default="{ uischema, schema }"
    :key="index"
    :value="value"
    @update-value="
      handleChange(`${control.path}.values.${index}.value`, $event)
    "
  >
    <DispatchRenderer
      :schema="schema"
      :uischema="uischema"
      :path="`${control.path}.values.${index}`"
      :enabled="control.enabled"
      :renderers="control.renderers"
      :cells="control.cells"
    />
  </DynamicValueControl>
</template>
