<script setup lang="ts">
import { type Directive, ref, toRef, watch } from "vue";
import * as monaco from "monaco-editor";

import { KdsEmptyState } from "@knime/kds-components";

import { useMainCodeEditorStore } from "../editor";
import { currentInputOutputItems } from "../store/ai-bar";
import { useInputOutputSelectionStore } from "../store/io-selection";

import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "./InputOutputItem.vue";
import useShouldFocusBePainted from "./utils/shouldFocusBePainted";

const emit = defineEmits<{
  "drop-event-handler-created": [
    dropEventHandler: (payload: DragEvent) => void,
  ];
  "input-output-item-insertion": [
    codeToInsert: string,
    requiredImport: string | undefined,
  ];
}>();

type Props = { inputOutputItems?: InputOutputModel[] };
const props = withDefaults(defineProps<Props>(), {
  inputOutputItems: () => [],
});

// Watch inputOutputItems and update store/ai-bar/currentInputOutputItems
const inputOutputItemsRef = toRef(props, "inputOutputItems");

watch(
  inputOutputItemsRef,
  (newItems) => {
    currentInputOutputItems.value = newItems;
  },
  { immediate: true },
);

const inputOutputSelectionStore = useInputOutputSelectionStore();

const selectedItemIndex = ref<number>(0);
const selectableItems = ref<(typeof InputOutputItem)[]>();

// Directive that removes element plus all children from tab flow. We will apply to all InputOutputItems.
const vRemoveFromTabFlow: Directive = {
  mounted: (thisElement: Element) => {
    thisElement.setAttribute("tabindex", "-1");

    const focusableElements = thisElement.querySelectorAll(
      'a, button, input, textarea, select, details, [tabindex]:not([tabindex="-1"])',
    );
    focusableElements.forEach((childElement: Element) => {
      childElement.setAttribute("tabindex", "-1");
    });
  },
};

const mainEditorState = useMainCodeEditorStore();

const dropEventHandler = (event: DragEvent) => {
  // If source is not input/output element, do nothing
  if (event.dataTransfer?.getData("eventId") !== INPUT_OUTPUT_DRAG_EVENT_ID) {
    return;
  }

  // check if an import is required for the selected item
  const requiredImport =
    inputOutputSelectionStore.selectedItem?.requiredImport ?? null;

  if (
    requiredImport &&
    !mainEditorState.value?.text.value.includes(requiredImport)
  ) {
    // wait until monaco has processed drop event
    const disposer =
      mainEditorState.value?.editor.value?.onDidChangeModelContent(() => {
        disposer?.dispose();

        const addImportEdit = {
          range: new monaco.Range(1, 1, 1, 1),
          text: `${requiredImport}\n`,
          forceMoveMarkers: true,
        };
        mainEditorState.value?.editorModel?.pushEditOperations(
          [],
          [addImportEdit],
          () => null,
        );
      });
  }

  // clear selection
  delete inputOutputSelectionStore.selectedItem;
};

emit("drop-event-handler-created", dropEventHandler);

const paintFocus = useShouldFocusBePainted();

const handleKeyDown = (e: KeyboardEvent) => {
  switch (e.key) {
    case "ArrowDown":
      selectedItemIndex.value =
        (selectedItemIndex.value + 1) % props.inputOutputItems.length;
      break;
    case "ArrowUp":
      selectedItemIndex.value =
        (selectedItemIndex.value - 1 + props.inputOutputItems.length) %
        props.inputOutputItems.length;
      break;
    case "Enter":
    case " ":
      selectableItems.value?.[selectedItemIndex.value].toggleExpansion();
      break;
    case "ArrowRight":
      selectableItems.value?.[selectedItemIndex.value].setExpanded(true);
      break;
    case "ArrowLeft":
      selectableItems.value?.[selectedItemIndex.value].setExpanded(false);
      break;
    case "Home":
      selectedItemIndex.value = 0;
      break;
    case "End":
      selectedItemIndex.value = props.inputOutputItems.length - 1;
      break;
  }

  if (e.key !== "Tab") {
    e.preventDefault(); // Stop accidental scrolling (but don't break tab navigation)
  }
};

const handleOnClick = (
  codeToInsert: string,
  requiredImport: string | undefined,
) => {
  emit("input-output-item-insertion", codeToInsert, requiredImport);
};
</script>

<template>
  <div
    v-if="inputOutputItems.length > 0"
    class="in-out-container"
    tabindex="0"
    role="menu"
    @keydown="handleKeyDown"
  >
    <InputOutputItem
      v-for="(inputOutputItem, i) in props.inputOutputItems"
      :key="inputOutputItem.name"
      ref="selectableItems"
      v-remove-from-tab-flow
      :input-output-item="inputOutputItem"
      :class="{
        'keyboard-selected': i === selectedItemIndex,
        'focus-painted': paintFocus,
      }"
      @input-output-item-clicked="handleOnClick"
    />
  </div>
  <div v-else class="empty-state-container">
    <KdsEmptyState
      headline="No data connected"
      description="Connect data to the port and get started."
    />
  </div>
</template>

<style scoped lang="postcss">
.in-out-container {
  display: flex;
  flex-direction: column;
  min-width: 150px;
  height: 100%;
  padding: var(--kds-spacing-container-0-5x);
  gap: var(--kds-spacing-container-0-10x);
  background-color: var(--kds-color-surface-default);
}

.in-out-container:focus-within :deep(.focus-painted.keyboard-selected::after) {
  content: "";
  position: absolute;
  inset: 0;
  border: 2px solid var(--knime-cornflower);
  pointer-events: none;
  z-index: 1;
}

.in-out-container:focus {
  outline: none;
}

.empty-state-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
