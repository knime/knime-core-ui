<script lang="ts">
import type { Component } from "vue";

export type SubItem<PropType extends Record<string, any>> = {
  name: string;
  /**
   * The type of the subItem. Can be a string, in which case that is displayed as
   * the type, or a component with optional props, in which case the component is
   * responsible for displaying the type.
   */
  type:
    | string
    | {
        component: Component;
        props?: PropType;
      };
  /**
   * Whether the subItem is supported in the current editor or not. If not, it is
   * displayed differently and does not support insertion.
   */
  supported: boolean;
  /**
   * For supported editors, setting this to any truthy string value will insert only
   * that value, with no extra decorations or delimiters.
   */
  insertionText?: string | null;
};

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
  /**
   * List of SubItems for this I/O-Object
   */
  subItems?: SubItem<Record<string, any>>[];
};

export const INPUT_OUTPUT_DRAG_EVENT_ID = "input_output_drag_event";
export const COLUMN_INSERTION_EVENT = "columnInsertion";
</script>

<script setup lang="ts">
import { useInputOutputSelectionStore } from "@/store/io-selection";
import Handlebars from "handlebars";
import { ref, watch } from "vue";
import { Collapser, PortIcon, useMultiSelection } from "@knime/components";
import { createDragGhost, removeDragGhost } from "./utils/dragGhost";
import { insertionEventHelper } from "@/components/utils/insertionEventHelper";
import EyeIcon from "@knime/styles/img/icons/eye.svg";
import { useReadonlyStore } from "@/store/readOnly";

const INITIALLY_EXPANDED_MAX_SUBITEMS = 15;

type PropType = {
  inputOutputItem: InputOutputModel;
};

const props = defineProps<PropType>();

const multiSelection = useMultiSelection({
  singleSelectionOnly: ref(!props.inputOutputItem.multiSelection),
});

Handlebars.registerHelper("escapeDblQuotes", (str: string) => {
  return str.replace("\\", "\\\\").replace(/"/g, '\\"');
});
const subItemCodeAliasTemplate = Handlebars.compile(
  props.inputOutputItem.subItemCodeAliasTemplate ?? "",
);

const inputOutputSelectionStore = useInputOutputSelectionStore();

// Reset selection if another item is selected
watch(
  () => inputOutputSelectionStore.selectedItem,
  (newItem) => {
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
    subItems: [props.inputOutputItem.subItems?.[index]],
  });

  insertionEventHelper
    .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
    .handleInsertion({
      textToInsert: codeToInsert,
      extraArgs: {
        requiredImport: props.inputOutputItem.requiredImport,
      },
    });
};

const handleHeaderDoubleClick = () => {
  // Only do something if we have a defined code alias
  if (props.inputOutputItem.codeAlias) {
    const codeToInsert = props.inputOutputItem.codeAlias;
    insertionEventHelper
      .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
      .handleInsertion({
        textToInsert: codeToInsert,
        extraArgs: {
          requiredImport: props.inputOutputItem.requiredImport,
        },
      });
  }
};

const getSubItemCodeToInsert = () => {
  const subItems = [...multiSelection.selectedIndexes.value]
    .filter((item) => props.inputOutputItem.subItems?.[item].supported)
    .map((item) => props.inputOutputItem.subItems?.[item]);

  const codeToInsert = subItemCodeAliasTemplate({ subItems });

  return codeToInsert;
};

const onSubItemDragStart = (event: DragEvent, index: number) => {
  inputOutputSelectionStore.selectedItem = props.inputOutputItem;

  if (!multiSelection.isSelected(index)) {
    multiSelection.resetSelection();
    multiSelection.handleSelectionClick(index);
  }

  const draggedItem = props.inputOutputItem.subItems?.[index]!;
  const dragGhost = createDragGhost({
    elements: [{ text: draggedItem.name }],
    numSelectedItems: multiSelection.selectedIndexes.value.filter(
      (item) => props.inputOutputItem.subItems?.[item].supported,
    ).length,
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

const globalReadOnly = useReadonlyStore();
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
            disabled: globalReadOnly,
          }"
          :draggable="!globalReadOnly"
          @mousedown="(event) => handleClick(event)"
          @dblclick="handleHeaderDoubleClick"
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
          disabled: !subItem.supported || globalReadOnly,
        }"
      >
        <span
          class="sub-item-name"
          :class="{
            draggable:
              inputOutputItem.subItemCodeAliasTemplate &&
              !globalReadOnly &&
              subItem.supported,
          }"
          :draggable="
            Boolean(
              inputOutputItem.subItemCodeAliasTemplate &&
                !globalReadOnly &&
                subItem.supported,
            )
          "
          @dragstart="(event) => onSubItemDragStart(event, index)"
          @dragend="onSubItemDragEnd"
          @click="(event) => handleClick(event, index)"
          @dblclick="handleSubItemDoubleClick($event, index)"
        >
          <div
            :class="{
              interactive: props.inputOutputItem.subItemCodeAliasTemplate,
              selected:
                props.inputOutputItem.subItemCodeAliasTemplate &&
                props.inputOutputItem.multiSelection &&
                multiSelection.isSelected(index) &&
                subItem.supported,
            }"
          >
            {{ subItem.name }}
          </div>
        </span>
        <div class="subitem-type">
          <component
            :is="subItem.type.component"
            v-if="typeof subItem.type !== 'string'"
            v-bind="subItem.type.props"
          />
          <span v-else>{{ subItem.type }}</span>
        </div>
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
        disabled: globalReadOnly,
      }"
      :draggable="!globalReadOnly"
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
  padding-left: var(--space-8);
  position: relative;
  margin: 0;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: var(--space-8);
  align-items: center;
  line-height: 26px;
  overflow: auto;
  padding-right: var(--space-32);
}

.port-icon-container {
  width: var(--in-out-item-icon-size);
  min-width: var(--in-out-item-icon-size);
  height: var(--in-out-item-icon-size);
  display: inline-block;
  overflow: hidden;
  position: relative;
  margin-right: var(--space-4);

  & svg {
    width: var(--in-out-item-icon-size);
    height: var(--in-out-item-icon-size);
    display: block;
    top: 0;
    left: 0;
    margin: 0;
  }
}

.title {
  flex-basis: calc(100px + var(--in-out-item-icon-size) + var(--space-4));
  min-width: 60px;
  align-items: center;
  display: flex;
  text-wrap: nowrap;
  text-overflow: ellipsis;
  padding-right: var(--space-4);
  font-weight: 500;
}

.collapser-content {
  font-size: 11px;
  width: 100%;
}

.sub-item {
  width: 100%;
  display: flex;
  flex-direction: row;
  gap: var(--space-8);
  height: 24px;
  line-height: 18px;
  align-items: center;
  padding-right: 10px;
  padding-left: 2px;

  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }
}

.sub-item-name {
  flex-grow: 1;
  min-width: 0;

  &.draggable {
    cursor: grab;
  }
}

.selected {
  background-color: var(--knime-cornflower-semi);
}

.interactive {
  background-color: transparent;
  border-radius: 30px;
  transition: background-color 0.1s ease;
  min-width: 0;
  text-wrap: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: min-content;
  max-width: 100%;
  padding: 2px var(--space-8);

  &.selected {
    background-color: var(--knime-cornflower-semi);
  }
}

.sub-item-name:hover .clickable-sub-item {
  background-color: var(--knime-stone-light);
  cursor: grab;
}

.subitem-type {
  font-style: italic;
  text-align: end;
  min-width: min-content;
  flex-grow: 0;
  text-wrap: nowrap;
}

.code-alias {
  font-family: monospace;
  font-weight: normal;
  font-size: 10px;
  line-height: var(--space-16);
  padding-left: var(--space-8);
  padding-right: var(--space-8);
  text-overflow: ellipsis;
  overflow: hidden;
  flex-shrink: 10;
  cursor: grab;
  border-radius: 30px;

  &:active {
    cursor: grabbing;
  }

  &:hover {
    background-color: var(--knime-stone-light);
    box-shadow: 1px 1px 4px 1px hsl(195deg 2% 52% / 40%);
  }
}

.code-alias-dragging {
  background-color: var(--knime-stone-light);
  box-shadow: 1px 1px 4px 1px hsl(195deg 2% 52% / 40%);
}

.collapser {
  position: relative;

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
