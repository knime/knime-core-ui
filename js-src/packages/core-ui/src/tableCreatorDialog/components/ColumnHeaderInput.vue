<script setup lang="ts">
import { useTemplateRef } from "vue";
import type { ColumnParameters } from "../types";
import ColumnNameInput from "./ColumnNameInput.vue";
import ColumnTypeInput from "./ColumnTypeInput.vue";
import type { ColumnType } from "@knime/knime-ui-table";

const props = defineProps<{
  columnData: ColumnParameters;
  /**
   * We the state provider mechanism for the column type choices.
   * Since it is only one provided state, we can directly pass the one initially updated value here and
   * do not keep track of different provided states.
   */
  dataTypePossibleValues: any;
  otherColumnNames: Set<string>;
}>();


const emit = defineEmits<{
  "update:columnName": [string];
  "update:columnType": [ColumnType];
  "columnNameKeydownEnter": [];
  "columnNameFocusOut": [];
}>();

const columnNameInputRef = "columnNameInput"
const columnNameInput = useTemplateRef<typeof ColumnNameInput>(columnNameInputRef);

const focusColumnNameInput = (initialValue?: string) => {
  columnNameInput.value?.focus(
  );
  if (initialValue !== undefined) {
   props.columnData.name = initialValue;
  }
};


defineExpose({ focusColumnNameInput });

</script>

<template>
  <div class="column-header-inputs">
  <ColumnNameInput
    :model-value="columnData.name"
    :ref="columnNameInputRef"
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
@import './errorMessage/error-messages.css';
</style>

<style lang="postcss" scoped>

.column-header-inputs {
  display: flex;
  flex-direction: column;
  padding: var(--space-16) var(--space-16) var(--error-message-min-reserved-space);
  gap: var(--error-message-min-reserved-space);
}

</style>
