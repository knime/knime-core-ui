<script lang="ts">
export type InputOutputModel = {
  name: string;
  /**
   * Code alias for inserting the entire I/O-Object
   */
  codeAlias?: string;
  /**
   * Template string for inserting one or multiple columns of the I/O-Object
   */
  subItemCodeAliasTemplate?: string;
  /**
   * If an import is required to use this I/O-Object, this import string will be pasted
   * at the beginning of the script if it is not present
   */
  requiredImport?: string;
  /**
   * Whether multi selection of subItems is supported or not
   */
  multiSelection?: boolean;
  subItems?: {
    name: string;
    type: string;
  }[];
};
export const INPUT_OUTPUT_DRAG_EVENT_ID = "input_output_drag_event";
</script>

<script setup lang="ts">
import { useInputOutputSelectionStore } from "@/store/io-selection";
import Handlebars from "handlebars";
import { ref, watch } from "vue";
import Collapser from "webapps-common/ui/components/Collapser.vue";
import { useMultiSelection } from "webapps-common/ui/components/FileExplorer/useMultiSelection";
import { createDragGhost, removeDragGhost } from "./utils/dragGhost";

const INITIALLY_EXPANDED_MAX_SUBITEMS = 15;

const props = defineProps<{
  inputOutputItem: InputOutputModel;
}>();

const multiSelection = useMultiSelection({
  singleSelectionOnly: ref(!props.inputOutputItem.multiSelection),
});

const draggedItem = ref<{ name: string; type: string }>({
  name: "",
  type: "",
});
const subItemCodeAliasTemplate = Handlebars.compile(
  props.inputOutputItem.subItemCodeAliasTemplate ?? "",
);

const inputOutputSelectionStore = useInputOutputSelectionStore();

// Reset selection if another item is selected
watch(
  () => inputOutputSelectionStore.selectedItem,
  (newItem, oldItem) => {
    if (newItem !== props.inputOutputItem) {
      multiSelection.resetSelection();
    }
  },
);

const handleClick = (event: MouseEvent, index?: number) => {
  event.stopPropagation();

  // This is the selected item now - resets the selection on all other items
  inputOutputSelectionStore.selectedItem = props.inputOutputItem;

  // Handle multi selection state
  if (typeof index === "undefined") {
    // Click on the header
    multiSelection.resetSelection();
  } else {
    // Click on a subItem
    multiSelection.handleSelectionClick(index, event);
  }
};

const getSubItemCodeToInsert = () => {
  const subItems = [...multiSelection.selectedIndexes.value].map(
    (item) => props.inputOutputItem.subItems?.[item].name,
  );
  const codeToInsert = subItemCodeAliasTemplate({ subItems });
  return codeToInsert;
};

const onSubItemDragStart = (event: DragEvent, index: number) => {
  inputOutputSelectionStore.selectedItem = props.inputOutputItem;

  if (!multiSelection.isSelected(index)) {
    multiSelection.resetSelection();
    multiSelection.handleSelectionClick(index);
  }
  draggedItem.value = props.inputOutputItem.subItems?.[index]!;
  const width = (event.target as any).offsetWidth;
  const dragGhost = createDragGhost({
    width: `${width}px`,
    elements: [
      { text: draggedItem.value.name },
      { text: draggedItem.value.type },
    ],
    numSelectedItems: multiSelection.selectedIndexes.value.length,
  });
  event.dataTransfer?.setDragImage(dragGhost, 0, 0);
  const codeToInsert = getSubItemCodeToInsert();
  event.dataTransfer?.setData("text", codeToInsert);
  event.dataTransfer?.setData("eventId", INPUT_OUTPUT_DRAG_EVENT_ID);
};

const onSubItemDragEnd = () => {
  removeDragGhost();
};

const isDraggingHeader = ref(false);
const onHeaderDragStart = (event: DragEvent, codeAlias: string) => {
  isDraggingHeader.value = true;
  const dragGhost = createDragGhost({
    width: "auto",
    elements: [{ text: props.inputOutputItem.codeAlias! }],
    numSelectedItems: 1,
    font: "monospace",
  });
  event.dataTransfer?.setDragImage(dragGhost, 0, 0);
  event.dataTransfer?.setData("text", codeAlias);
  event.dataTransfer?.setData("eventId", INPUT_OUTPUT_DRAG_EVENT_ID);
};

const onHeaderDragEnd = () => {
  isDraggingHeader.value = false;
  removeDragGhost();
};
</script>

<template>
  <Collapser
    v-if="inputOutputItem.subItems"
    :initially-expanded="
      inputOutputItem.subItems?.length <= INITIALLY_EXPANDED_MAX_SUBITEMS
    "
    class="collapser bottom-border"
  >
    <template #title>
      <div class="top-card has-collapser">
        <div class="title">
          {{ inputOutputItem.name }}
        </div>
        <div
          v-if="inputOutputItem.codeAlias"
          class="code-alias"
          :class="{
            'code-alias-dragging': isDraggingHeader,
            'code-alias-not-dragging': !isDraggingHeader,
          }"
          :draggable="true"
          @mousedown="(event) => handleClick(event)"
          @dragstart="
            (event) => onHeaderDragStart(event, inputOutputItem.codeAlias!)
          "
          @dragend="onHeaderDragEnd"
        >
          {{ inputOutputItem.codeAlias }}
        </div>
      </div>
    </template>
    <div v-if="props.inputOutputItem.subItems" class="collapser-content">
      <div
        v-for="(subItem, index) in props.inputOutputItem.subItems"
        :key="index"
        class="sub-item"
        :class="{
          'clickable-sub-item': props.inputOutputItem.subItemCodeAliasTemplate,
          selected:
            props.inputOutputItem.subItemCodeAliasTemplate &&
            multiSelection.isSelected(index),
        }"
        :draggable="Boolean(props.inputOutputItem.subItemCodeAliasTemplate)"
        @dragstart="(event) => onSubItemDragStart(event, index)"
        @dragend="onSubItemDragEnd"
        @click="(event) => handleClick(event, index)"
      >
        <div class="cell">{{ subItem.name }}</div>
        <div class="cell">{{ subItem.type }}</div>
      </div>
    </div>
  </Collapser>
  <div v-else class="top-card bottom-border">
    <div class="title">
      {{ inputOutputItem.name }}
    </div>
    <div
      v-if="inputOutputItem.codeAlias"
      class="code-alias"
      :class="{
        'code-alias-dragging': isDraggingHeader,
        'code-alias-not-dragging': !isDraggingHeader,
      }"
      :draggable="true"
      @mousedown="(event) => handleClick(event)"
      @dragstart="
        (event) => onHeaderDragStart(event, inputOutputItem.codeAlias!)
      "
      @dragend="onHeaderDragEnd"
    >
      {{ inputOutputItem.codeAlias }}
    </div>
  </div>
</template>

<style scoped lang="postcss">
.top-card {
  min-height: 42px;
  background-color: var(--knime-porcelain);
  padding-left: 8px;
  position: relative;
  margin: 0;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  flex-direction: row;
  justify-content: left;
  align-items: center;
  line-height: 26px;
  overflow: auto;
}

.has-collapser {
  padding-right: 32px;
}

.title {
  flex-basis: 100px;
  min-width: 60px;
}

.collapser-content {
  font-size: 11px;
  width: 100%;
}

.sub-item {
  width: 100%;
  border-bottom: 1px solid var(--knime-porcelain);
  display: flex;
  flex-direction: row;
}

.selected {
  background-color: var(--knime-cornflower-semi);
}

.clickable-sub-item {
  cursor: grab;

  &:active {
    cursor: grabbing;
  }

  &:hover {
    box-shadow: 1px 1px 4px 1px hsl(195deg 2% 52% / 40%);
  }
}

.cell {
  padding: 10px;
  flex: 50%;
}

.code-alias {
  font-family: monospace;
  font-weight: normal;
  font-size: 12px;
  padding-left: 8px;
  padding-right: 8px;
  text-overflow: ellipsis;
  overflow: hidden;
  flex-shrink: 10;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

.code-alias-dragging {
  background-color: var(--knime-cornflower-semi);
  box-shadow: 1px 1px 4px 1px hsl(195deg 2% 52% / 40%);
}

.code-alias-not-dragging {
  &:hover {
    background-color: var(--knime-stone-light);
    box-shadow: 1px 1px 4px 1px hsl(195deg 2% 52% / 40%);
  }
}

.collapser {
  position: relative;
  z-index: 10;

  & :deep(.dropdown) {
    width: 20px;
    height: 20px;
    top: 10px;

    & .dropdown-icon {
      width: 12px;
      height: 12px;
    }
  }

  & :deep(.panel) {
    &:hover {
      overflow: visible;
    }
  }
}

.bottom-border {
  border-bottom: 1px solid var(--knime-white);
}
</style>
