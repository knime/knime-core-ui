<script setup lang="ts">
import { Label } from "@knime/components";
import type { ColumnType } from "@knime/knime-ui-table";

import type { DataTypeChoice } from "./DataTypeDropdown.vue";
import DataTypeDropdown from "./DataTypeDropdown.vue";

const props = defineProps<{
  possibleValues: DataTypeChoice[];
}>();
const modelValue = defineModel<ColumnType>({
  required: true,
});
</script>

<template>
  <Label text="Column type">
    <template #default="{ labelForId }">
      <DataTypeDropdown
        v-bind="{ ...props, ariaLabel: 'Select column type' }"
        :id="labelForId"
        v-model="modelValue"
      />
    </template>
  </Label>
</template>
