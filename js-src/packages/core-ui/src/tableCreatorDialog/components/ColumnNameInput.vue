<script setup lang="ts">
import { InputField, Label } from "@knime/components";
import ErrorMessages from "./errorMessage/ErrorMessages.vue";
import { computed, useTemplateRef } from "vue";

const props = defineProps<{
  otherColumnNames: Set<string>;
}>();


const validations: [isInvalidCallback: (value: string) => boolean, errorMessage: string][] = [
  [(value) => value === "", "Column name cannot be empty."],
  [(value) => value.trim() === "", "Column name cannot be blank."],
  [(value) => value.trim() !== value, "Column name cannot have leading or trailing spaces."],
  [(value) => props.otherColumnNames.has(value), "Duplicate column names are not allowed."],
];
const errorMessage = computed(() => {
  const value = modelValue.value;
  return validations.find(([isInvalid]) => isInvalid(value))?.[1];
});

const errors = computed(() => errorMessage.value ? [errorMessage.value] : []);

const modelValue = defineModel<string>({
  required: true,
});

const inputFieldRef = "inputField"
const inputField = useTemplateRef<typeof InputField>(inputFieldRef);


defineExpose({
  focus: () =>  inputField.value?.$refs.input.focus({
    preventScroll: true,
  })
})
</script>

<template>
  <Label text="Column name">
    <template #default="{ labelForId }">
      <ErrorMessages :errors>
        <InputField :ref="inputFieldRef" compact v-model="modelValue" :is-valid="errors.length === 0"/>
      </ErrorMessages>
    </template>
  </Label>
</template>
