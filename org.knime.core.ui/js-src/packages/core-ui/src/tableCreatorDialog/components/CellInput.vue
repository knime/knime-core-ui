<script setup lang="ts">
import { computed, nextTick, useTemplateRef } from "vue";

import { TextArea } from "@knime/components";
import {
  KdsValueSwitch,
  type KdsValueSwitchOption,
} from "@knime/kds-components";

import type { CellData } from "../composables/useTableConfig";

const modelValue = defineModel<CellData | null>({ required: true });

const stringValue = computed<string>({
  get: () => modelValue.value?.value ?? "",
  set: (val: string) => {
    modelValue.value = { value: val, isValid: true };
  },
});

type MissingSwitchValue = "VALUE" | "MISSING";
const valueSwitchPossibleValues: (KdsValueSwitchOption & {
  id: MissingSwitchValue;
})[] = [
  { id: "VALUE", text: "Value" },
  { id: "MISSING", text: "Missing value", leadingIcon: "circle-question" },
];

const inputFieldRef = "inputField";
const inputField = useTemplateRef<TextArea>(inputFieldRef);

const focusValueInput = () => {
  inputField.value?.$refs.input.focus();
};

const missingSwitchValue = computed<MissingSwitchValue>({
  // eslint-disable-next-line no-undefined
  get: () => (modelValue.value?.value === undefined ? "MISSING" : "VALUE"),
  set: (val: MissingSwitchValue) => {
    if (val === "MISSING") {
      modelValue.value = null;
    } else {
      stringValue.value = stringValue.value || "";
      nextTick(focusValueInput);
    }
  },
});

const isValid = computed(() => modelValue.value?.isValid ?? true);
</script>

<template>
  <div class="cell-input">
    <KdsValueSwitch
      v-model="missingSwitchValue"
      class="value-switch"
      size="medium"
      :possible-values="valueSwitchPossibleValues"
    />
    <TextArea
      v-if="missingSwitchValue === 'VALUE'"
      :ref="inputFieldRef"
      v-model="stringValue"
      class="text-area-input"
      :is-valid="isValid"
      :rows="5"
      :cols="18"
      @pointerdown.stop
    />
  </div>
</template>

<style lang="postcss" scoped>
.value-switch {
  margin-bottom: 8px;
}

.text-area-input {
  max-width: 100%;

  & :deep(textarea) {
    width: 100%;
    min-height: 40px;
    padding: 10px;
    resize: vertical;
  }
}
</style>
