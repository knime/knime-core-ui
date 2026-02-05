<script setup lang="ts">
import { TextArea, useClickOutside } from '@knime/components';
import { onMounted, useTemplateRef, type Ref, toRef, ref, computed } from 'vue';
import { autoUpdate, useFloating , offset } from '@floating-ui/vue';
import type { CellData } from './TableCreatorDialog.vue';

const modelValue = defineModel<CellData>();

const stringValue = computed<string>({
    get: () => modelValue.value?.value ?? '',
    set: (val: string) => {
        // Set isValid: true optimistically - async validation will update later if invalid
        modelValue.value = { value: val, isValid: true };
    },
});

const isValid = computed(() => modelValue.value?.isValid ?? true);
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
        stringValue.value =  props.initialValue;
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
        <TextArea
            :ref="inputFieldRef"
            v-model="stringValue"
            :is-valid="isValid"
            :rows="1"
            :cols="18"
            @pointerdown.stop
        />
    </div>
</template>

<style scoped lang="postcss">

.textArea {
    position: fixed;
    z-index: 10;
}
</style>