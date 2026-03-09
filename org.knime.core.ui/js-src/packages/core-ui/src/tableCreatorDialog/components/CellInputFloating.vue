<script setup lang="ts">
import { type Ref, computed, onMounted, ref, toRef, useTemplateRef } from "vue";
import { autoUpdate, offset, useFloating } from "@floating-ui/vue";

import { TextArea } from "@knime/components";

import type { CellData } from "../composables/useTableConfig";

const modelValue = defineModel<CellData>({ required: true });

const stringValue = computed<string>({
  get: () => modelValue.value?.value ?? "",
  set: (val: string) => {
    // Set isValid: true optimistically - async validation will update later if invalid
    modelValue.value = { value: val, isValid: true };
  },
});

const isValid = computed(() => modelValue.value?.isValid ?? true);
const props = defineProps<{
  initialValue?: string;
  referenceElement: HTMLElement;
  onClickAway?: (rowInd: number, colInd: number) => void;
  rowInd: number;
  colInd: number;
}>();

const initialRowInd = props.rowInd;
const initialColInd = props.colInd;

const clickAwayWithIndices = () => {
  props.onClickAway?.(initialRowInd, initialColInd);
};

const inputFieldRef = "inputField";
const inputField = useTemplateRef<TextArea>(inputFieldRef);
const floatingElementRef = "floatingElement";
const floatingElement: Ref<HTMLElement | null> =
  useTemplateRef<HTMLElement>(floatingElementRef);

const referenceEl = toRef(props, "referenceElement");

const { x, y } = useFloating(referenceEl, floatingElement, {
  strategy: "fixed",
  placement: "bottom-start",
  whileElementsMounted: autoUpdate,
  middleware: [offset(({ rects }) => -rects.reference.height)],
});

const isActive = ref(false);

onMounted(() => {
  if (typeof props.initialValue === "string") {
    stringValue.value = props.initialValue;
  }
  inputField.value?.$refs.input.focus();
  setTimeout(() => {
    isActive.value = true;
  }, 0);
});

const close = () => {
  if (isActive.value) {
    clickAwayWithIndices();
  }
};
</script>

<template>
  <div
    :ref="floatingElementRef"
    class="text-area"
    :style="{ left: `${x}px`, top: `${y}px` }"
    @focusout="close"
  >
    <TextArea
      :ref="inputFieldRef"
      v-model="stringValue"
      :is-valid="isValid"
      :rows="1"
      :cols="14"
      @pointerdown.stop
    />
  </div>
</template>

<style scoped lang="postcss">
.text-area {
  position: fixed;
  z-index: 10;
}
</style>
