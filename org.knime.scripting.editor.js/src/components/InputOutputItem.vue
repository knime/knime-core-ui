<script lang="ts">
export type InputOutputModel = {
  name: string;
  /**
   * Represents the type of the item, e.g. input table, flow variable, port object...
   */
  portType?: "table" | "flowVariable" | "object" | "view";
  /**
   * Color of the port icon
   */
  portIconColor?: string;
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
import PortIcon from "webapps-common/ui/components/node/PortIcon.vue";
import EyeIcon from "webapps-common/ui/assets/img/icons/eye.svg";

const INITIALLY_EXPANDED_MAX_SUBITEMS = 15;

const props = defineProps<{
  inputOutputItem: InputOutputModel;
}>();

const emit = defineEmits(["input-output-item-clicked"]);

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

const collapserRef = ref();

const toggleExpansion = () => {
  if (collapserRef.value) {
    collapserRef.value.isExpanded = !collapserRef.value.isExpanded;
  }
};

const isExpanded = () => {
  return collapserRef.value?.isExpanded;
};

const setExpanded = (b: boolean) => {
  if (collapserRef.value) {
    collapserRef.value.isExpanded = b;
  }
};

defineExpose({
  toggleExpansion,
  setExpanded,
  isExpanded,
});

const handleClick = (event: MouseEvent, index?: number) => {
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

const handleSubItemDoubleClick = (event: MouseEvent, index: number) => {
  const codeToInsert = subItemCodeAliasTemplate({
    subItems: [props.inputOutputItem.subItems?.[index]?.name],
  });

  emit(
    "input-output-item-clicked",
    codeToInsert,
    props.inputOutputItem.requiredImport,
  );
};

const handleHeaderDoubleClick = (event: MouseEvent) => {
  // Only do something if we have a defined code alias
  if (props.inputOutputItem.codeAlias) {
    const codeToInsert = props.inputOutputItem.codeAlias;
    emit(
      "input-output-item-clicked",
      codeToInsert,
      props.inputOutputItem.requiredImport,
    );
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
    v-if="inputOutputItem.subItems?.length"
    ref="collapserRef"
    :initially-expanded="
      inputOutputItem.subItems?.length <= INITIALLY_EXPANDED_MAX_SUBITEMS
    "
    class="collapser bottom-border"
  >
    <template #title>
      <div class="top-card has-collapser">
        <div class="title">
          <div class="port-icon-container">
            <EyeIcon v-if="inputOutputItem.portType === 'view'" />
            <svg v-else viewBox="-6 -6 12 12">
              <PortIcon
                :type="inputOutputItem.portType"
                :color="inputOutputItem.portIconColor"
              />
            </svg>
          </div>
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
          @dblclick="handleHeaderDoubleClick($event)"
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
        @dblclick="handleSubItemDoubleClick($event, index)"
      >
        <div class="cell subitem-name">{{ subItem.name }}</div>
        <div class="cell subitem-type">{{ subItem.type }}</div>
      </div>
    </div>
  </Collapser>
  <div v-else class="top-card bottom-border">
    <div class="title">
      <div class="port-icon-container">
        <EyeIcon v-if="inputOutputItem.portType === 'view'" />
        <svg v-else viewBox="-6 -6 12 12">
          <PortIcon
            :type="inputOutputItem.portType"
            :color="inputOutputItem.portIconColor"
          />
        </svg>
      </div>
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
  --in-out-item-icon-size: 12px;

  min-height: 28px;
  background-color: var(--knime-porcelain);
  padding-left: 8px;
  position: relative;
  margin: 0;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
  line-height: 26px;
  overflow: auto;
  padding-right: 32px;
}

.port-icon-container {
  width: var(--in-out-item-icon-size);
  min-width: var(--in-out-item-icon-size);
  height: var(--in-out-item-icon-size);
  display: inline-block;
  overflow: hidden;
  position: relative;
  margin-right: var(--space-8);

  & svg {
    width: var(--in-out-item-icon-size);
    height: var(--in-out-item-icon-size);
    display: block;
    top: 0;
    left: 0;
    margin: 0;
  }
}

.subitem-type {
  font-style: italic;
}

.title {
  flex-basis: calc(100px + var(--in-out-item-icon-size) + var(--space-4));
  min-width: 60px;
  align-items: center;
  display: flex;
  text-wrap: nowrap;
  text-overflow: ellipsis;
  padding-right: 2px;
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
  justify-content: space-between;
  height: 22px;
  line-height: 18px;
  align-items: center;
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
  overflow: hidden;
  text-overflow: ellipsis;
  text-wrap: nowrap;
}

.code-alias {
  font-family: monospace;
  font-weight: normal;
  font-size: 12px;
  padding-left: var(--space-8);
  padding-right: var(--space-8);
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
    width: 14px;
    height: 14px;
    top: 7px;

    & .dropdown-icon {
      width: 10px;
      height: 10px;
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
