<script setup lang="ts">
import { TextArea, ValueSwitch } from "@knime/components";
import { computed, nextTick, useTemplateRef } from "vue";
import type { CellData } from "../composables/useTableConfig";

const modelValue = defineModel<CellData | null>({ required: true });

const stringValue = computed<string>({
  get: () => modelValue.value?.value ?? "",
  set: (val: string) => {
    modelValue.value = { value: val, isValid: true };
  },
});

type MissingSwitchValue = "VALUE" | "MISSING";
const valueSwitchPossibleValues: { id: MissingSwitchValue; text: string }[] = [
  { id: "VALUE", text: "Value" },
  { id: "MISSING", text: "Missing value" },
];

const missingSwitchValue = computed<MissingSwitchValue>({
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

const inputFieldRef = "inputField";
const inputField = useTemplateRef<TextArea>(inputFieldRef);

const focusValueInput = () => {
  inputField.value?.$refs.input.focus();
};
</script>

<template>
  <div class="cell-input">
    <ValueSwitch
      class="value-switch"
      compact
      v-model="missingSwitchValue"
      :possible-values="valueSwitchPossibleValues"
    />
    <TextArea
      v-if="missingSwitchValue === 'VALUE'"
      class="text-area-input"
      :ref="inputFieldRef"
      v-model="stringValue"
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
