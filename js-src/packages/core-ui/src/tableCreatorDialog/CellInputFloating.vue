<script setup lang="ts">
import { TextArea, useClickOutside } from '@knime/components';
import { onMounted, useTemplateRef, type Ref, toRef, ref } from 'vue';
import { autoUpdate, useFloating , offset } from '@floating-ui/vue';

const modelValue = defineModel<string>();
const props = defineProps<{
    initialValue?: string;
    referenceElement: HTMLElement;
    onClickAway?: () => void;
}>();

const inputFieldRef = 'inputField';
const inputField = useTemplateRef<TextArea>(inputFieldRef);
const floatingElementRef = 'floatingElement';
const floatingElement: Ref<HTMLElement | null> = useTemplateRef<HTMLElement>(floatingElementRef);

const referenceEl = toRef(props, 'referenceElement');

const { x, y } = useFloating(referenceEl, floatingElement, {
    strategy: 'fixed',
    placement: 'bottom-start',
    whileElementsMounted: autoUpdate,
    middleware: [
        offset(({ rects }) => -rects.reference.height),
    ],
});

const isActive = ref(false);
useClickOutside(
    { targets: [floatingElement], callback: () => props.onClickAway?.() },
    isActive,
);

onMounted(() => {
    if (props.initialValue) {
        modelValue.value = props.initialValue;
    }
    inputField.value?.$refs.input.focus();
    setTimeout(() => {
        isActive.value = true;
    }, 0);
});
</script>

<template>
    <div
        :ref="floatingElementRef"
        class="textArea"
        :style="{ left: `${x}px`, top: `${y}px` }"
    >
        <TextArea :ref="inputFieldRef" v-model="modelValue" :rows="1" :cols="18" @pointerdown.stop
        
        
        />
    </div>
</template>

<style scoped lang="postcss">

.textArea {
    position: fixed;
    z-index: 10;
}
</style>