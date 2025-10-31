<script setup lang="ts">
import { InputField, Label, NumberInput } from "@knime/components";
import { ErrorMessages } from "@knime/jsonforms";

withDefaults(
  defineProps<{
    disabled?: boolean;
    modelValue: { path: string; timeout: number };
    id: string | null;
    urlErrorMessage?: string | null;
  }>(),
  {
    disabled: false,
    urlErrorMessage: null,
  },
);
defineEmits(["update:path", "update:timeout"]);
</script>

<template>
  <ErrorMessages :errors="urlErrorMessage === null ? [] : [urlErrorMessage]">
    <InputField
      :id="id"
      compact
      :is-valid="urlErrorMessage === null"
      :disabled="disabled"
      :model-value="modelValue.path"
      placeholder="URL"
      @update:model-value="$emit('update:path', $event)"
    />
  </ErrorMessages>
  <Label #default="{ labelForId }" class="timeout" text="Timeout">
    <NumberInput
      :id="labelForId"
      type="integer"
      compact
      :min="0"
      :disabled="disabled"
      :model-value="modelValue.timeout"
      @update:model-value="$emit('update:timeout', $event)"
    />
  </Label>
</template>

<style scoped lang="postcss">
.timeout.timeout {
  margin-top: var(--error-message-min-reserved-space);
}
</style>
