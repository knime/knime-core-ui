<script setup lang="ts">
import { TextArea } from '@knime/components';
import { Form, LabeledControl } from '@knime/jsonforms';
import { computed, useTemplateRef } from 'vue';
import type { CellData } from './TableCreatorDialog.vue';

const modelValue = defineModel<CellData>();

const stringValue = computed<string>({
    get: () => modelValue.value?.value ?? '',
    set: (val: string) => {
        modelValue.value = { value: val, isValid: true };
    },
});


const inputFieldRef = 'inputField';
const inputField = useTemplateRef<TextArea>(inputFieldRef);

defineExpose({
    focus: () => {
        inputField.value?.$refs.input.focus();
    },
})

</script>

<template>
    <LabeledControl
        label="Value"
    >
    <template #default="{labelForId}">
        <TextArea 
        :id="labelForId"
            class="text-area-input"
            :ref="inputFieldRef"  v-model="stringValue" :rows="5" :cols="18" @pointerdown.stop
        />
    </template>
    </LabeledControl>
</template>

<style lang="postcss" scoped>
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