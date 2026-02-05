<script setup lang="ts">
import { TextArea } from '@knime/components';
import { Form, LabeledControl } from '@knime/jsonforms';
import { useTemplateRef } from 'vue';

const modelValue = defineModel<string>();


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
            :ref="inputFieldRef"  v-model="modelValue" :rows="5" :cols="18" @pointerdown.stop
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