<script setup lang="ts">
import { onMounted, ref, watch, type Ref, type Directive } from "vue";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "./InputOutputItem.vue";
import { getScriptingService } from "@/scripting-service";
import { useInputOutputSelectionStore } from "@/store/io-selection";
import { useMainCodeEditorStore } from "@/editor";
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
const inputOutputItems: Ref<InputOutputModel[]> = ref([]);
const inputOutputSelectionStore = useInputOutputSelectionStore();

const selectedItemIndex = ref<number>(0);
const selectableItems = ref<(typeof InputOutputItem)[]>();

const fetchInputOutputObjects = async (
  method: "getInputObjects" | "getOutputObjects",
) => {
  const items = await getScriptingService()[method]();
  if (items) {
    inputOutputItems.value.push(...items);
  }
};

const fetchFlowVariables = async () => {
  const item = await getScriptingService().getFlowVariableInputs();
  if (item) {
    inputOutputItems.value.push(item);
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
    const unwatch = watch(
      () => mainEditorState.value?.text.value,
      (newScript) => {
        unwatch();
        mainEditorState.value!.text.value = `${requiredImport}\n${newScript}`;
      },
    );
  }

  // clear selection
  delete inputOutputSelectionStore.selectedItem;
};

onMounted(async () => {
  await fetchInputOutputObjects("getInputObjects");
  await fetchFlowVariables();
  await fetchInputOutputObjects("getOutputObjects");
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
