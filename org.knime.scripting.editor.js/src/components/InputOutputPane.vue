<script setup lang="ts">
import { onMounted, ref, type Ref, type Directive } from "vue";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "./InputOutputItem.vue";
import { getInitialDataService } from "@/initial-data-service";
import { useInputOutputSelectionStore } from "@/store/io-selection";
import { useMainCodeEditorStore } from "@/editor";
import useShouldFocusBePainted from "./utils/shouldFocusBePainted";
import * as monaco from "monaco-editor";

const emit = defineEmits<{
  "drop-event-handler-created": [
    dropEventHandler: (payload: DragEvent) => void,
  ];
  "input-output-item-insertion": [
    codeToInsert: string,
    requiredImport: string | undefined,
  ];
}>();
const inputOutputItems: Ref<InputOutputModel[]> = ref([]);
const inputOutputSelectionStore = useInputOutputSelectionStore();

const selectedItemIndex = ref<number>(0);
const selectableItems = ref<(typeof InputOutputItem)[]>();

const fetchInitialData = async () => {
  const initialData = await getInitialDataService().getInitialData();
  inputOutputItems.value = [
    ...initialData.inputObjects,
    initialData.flowVariables,
  ];

  if (initialData.outputObjects) {
    inputOutputItems.value.push(...initialData.outputObjects);
  }
};

// Directive that removes element plus all children from tab flow. We will apply to all InputOutputItems.
const vRemoveFromTabFlow = {
  mounted: (thisElement: Element) => {
    thisElement.setAttribute("tabindex", "-1");

    const focusableElements = thisElement.querySelectorAll(
      'a, button, input, textarea, select, details, [tabindex]:not([tabindex="-1"])',
    );
    focusableElements.forEach((childElement: Element) => {
      childElement.setAttribute("tabindex", "-1");
    });
  },
} satisfies Directive;

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

onMounted(async () => {
  await fetchInitialData();
  emit("drop-event-handler-created", dropEventHandler);
});

const paintFocus = useShouldFocusBePainted();

const handleKeyDown = (e: KeyboardEvent) => {
  switch (e.key) {
    case "ArrowDown":
      selectedItemIndex.value =
        (selectedItemIndex.value + 1) % inputOutputItems.value.length;
      break;
    case "ArrowUp":
      selectedItemIndex.value =
        (selectedItemIndex.value - 1 + inputOutputItems.value.length) %
        inputOutputItems.value.length;
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
      selectedItemIndex.value = inputOutputItems.value.length - 1;
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
    class="in-out-container"
    tabindex="0"
    aria-role="menu"
    @keydown="handleKeyDown"
  >
    <InputOutputItem
      v-for="(inputOutputItem, i) in inputOutputItems"
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
</template>

<style scoped lang="postcss">
.in-out-container {
  display: flex;
  flex-direction: column;
  min-width: 150px;
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
</style>
