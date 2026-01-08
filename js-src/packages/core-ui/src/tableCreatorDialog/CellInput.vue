<script setup lang="ts">
import { TextArea } from '@knime/components';
import { onMounted, useTemplateRef, type Ref, toRef } from 'vue';
import { autoUpdate, useFloating , offset } from '@floating-ui/vue';

const modelValue = defineModel<string>();
const props = defineProps<{
    initialValue?: string;
    referenceElement: HTMLElement;
}>();

const ref = 'inputField';
const inputField = useTemplateRef<TextArea>(ref);
const floatingElement: Ref<HTMLElement | null> = useTemplateRef('floatingElement');

const referenceEl = toRef(props, 'referenceElement');

const { x, y } = useFloating(referenceEl, floatingElement, {
    strategy: 'fixed',
    placement: 'bottom-start',
    whileElementsMounted: autoUpdate,
    middleware: [
        offset(({ rects }) => -rects.reference.height),
    ],
});

onMounted(() => {
    if (props.initialValue) {
        modelValue.value = props.initialValue;
    }
    inputField.value?.$refs.input.focus();
});
</script>

<template>
    <div
        ref="floatingElement"
        class="textArea"
        :style="{ left: `${x}px`, top: `${y}px` }"
    >
        <TextArea :ref v-model="modelValue" :rows="1" :cols="18" @pointerdown.stop/>
    </div>
</template>

<style scoped lang="postcss">

.textArea {
    position: fixed;
    z-index: 10;
}
</style>