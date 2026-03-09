<script setup lang="ts">
import { useTemplateRef } from "vue";

import type { ColumnType } from "@knime/knime-ui-table";

import type { ColumnParameters } from "../types";

import ColumnNameInput from "./ColumnNameInput.vue";
import ColumnTypeInput from "./ColumnTypeInput.vue";

const props = defineProps<{
  columnData: ColumnParameters;
  dataTypePossibleValues: any;
  otherColumnNames: Set<string>;
}>();

const emit = defineEmits<{
  "update:columnName": [string];
  "update:columnType": [ColumnType];
  columnNameKeydownEnter: [];
  columnNameFocusOut: [];
}>();

const columnNameInputRef = "columnNameInput";
const columnNameInput =
  useTemplateRef<typeof ColumnNameInput>(columnNameInputRef);

const focusColumnNameInput = (initialValue?: string) => {
  columnNameInput.value?.focus();
  // eslint-disable-next-line no-undefined
  if (initialValue !== undefined) {
    props.columnData.name = initialValue; // eslint-disable-line vue/no-mutating-props
  }
};

defineExpose({ focusColumnNameInput });
</script>

<template>
  <div class="column-header-inputs">
    <ColumnNameInput
      :ref="columnNameInputRef"
      :model-value="columnData.name"
      :other-column-names="otherColumnNames"
      @update:model-value="emit('update:columnName', $event)"
      @keydown.enter.exact="emit('columnNameKeydownEnter')"
      @focusout="emit('columnNameFocusOut')"
    />
    <ColumnTypeInput
      :possible-values="dataTypePossibleValues"
      :model-value="columnData.type"
      @update:model-value="emit('update:columnType', $event)"
    />
  </div>
</template>

<style>
@import url("./errorMessage/error-messages.css");
</style>

<style lang="postcss" scoped>
.column-header-inputs {
  display: flex;
  flex-direction: column;
  padding: var(--space-16) var(--space-16)
    var(--error-message-min-reserved-space);
  gap: var(--error-message-min-reserved-space);
}
</style>
