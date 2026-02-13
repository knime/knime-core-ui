<script setup lang="ts">
import { TextArea, useClickOutside } from "@knime/components";
import { onMounted, useTemplateRef, type Ref, toRef, ref, computed, onUnmounted } from "vue";
import { autoUpdate, useFloating, offset } from "@floating-ui/vue";
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
  if (props.initialValue) {
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
}
</script>

<template>
  <div
    :ref="floatingElementRef"
    class="textArea"
    :style="{ left: `${x}px`, top: `${y}px` }"
    @focusout="close"
  >
    <TextArea
      :ref="inputFieldRef"
      v-model="stringValue"
      :is-valid="isValid"
      :rows="1"
      :cols="15"
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
