<script lang="ts">
/* eslint-disable max-lines */
export const INPUT_OUTPUT_DRAG_EVENT_ID = "input_output_drag_event";
export const COLUMN_INSERTION_EVENT = "columnInsertion";
</script>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { Component } from "vue";
import Handlebars, { type HelperOptions } from "handlebars";

import { BaseButton, PortIcon, useMultiSelection } from "@knime/components";
import { KdsDataType, KdsIcon } from "@knime/kds-components";

import { useInputOutputSelectionStore } from "../store/io-selection";
import { useReadonlyStore } from "../store/readOnly";

import { createDragGhost, removeDragGhost } from "./utils/dragGhost";
import { insertionEventHelper } from "./utils/insertionEventHelper";

export type SubItemType<DisplayName extends string = string> = {
  id?: string;
  title?: string;
  displayName: DisplayName;
};

export type SubItem<PropType extends Record<string, any>> = {
  name: string;
  /**
   * The type of the subItem that is displayed in the inputOutputPane
   */
  type: SubItemType;
  /**
   * An optional Component that is displayed before the name of the subItem.
   */
  icon?: {
    component: Component;
    props?: PropType;
  };
  /**
   * Whether the subItem is supported in the current editor or not. If not, it is
   * displayed differently and does not support insertion.
   */
  supported: boolean;
  /**
   * A text that is provided to the template when this sub item is inserted into
   * the code. Note that this is optional. The template also has access to the name
   * which can be enough.
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

/* eslint-disable @typescript-eslint/no-explicit-any */
const WHEN_OPERATORS = {
  eq: (l: any, r: any) => l === r,
  neq: (l: any, r: any) => l !== r,
  gt: (l: any, r: any) => Number(l) > Number(r),
  lt: (l: any, r: any) => Number(l) < Number(r),
  or: (l: any, r: any) => l || r,
  and: (l: any, r: any) => l && r,
} as const;
Handlebars.registerHelper(
  "when",
  // eslint-disable-next-line max-params
  function (
    this: any,
    leftOperand: any,
    operator: keyof typeof WHEN_OPERATORS,
    rightOperand: any,
    options: HelperOptions,
  ) {
    return WHEN_OPERATORS[operator](leftOperand, rightOperand)
      ? options.fn(this)
      : options.inverse(this);
  },
);
/* eslint-enable @typescript-eslint/no-explicit-any */
const subItemCodeAliasTemplate = Handlebars.compile(
  props.inputOutputItem.subItemCodeAliasTemplate ?? "",
);

const inputOutputSelectionStore = useInputOutputSelectionStore();

// Reset selection if another item is selected
watch(
  () => inputOutputSelectionStore.selectedItem,
  (newItem) => {
    if (newItem?.name !== props.inputOutputItem.name) {
      multiSelection.resetSelection();
    }
  },
);

const isExpanded = ref(
  (props.inputOutputItem.subItems?.length ?? 0) > 0 &&
    (props.inputOutputItem.subItems?.length ?? 0) <=
      INITIALLY_EXPANDED_MAX_SUBITEMS,
);

watch(
  () => props.inputOutputItem.subItems?.length,
  (newLength) => {
    if (!newLength) {
      isExpanded.value = false;
      return;
    }
    isExpanded.value = newLength <= INITIALLY_EXPANDED_MAX_SUBITEMS;
  },
);

const toggleExpansion = () => {
  isExpanded.value = !isExpanded.value;
};

const setExpanded = (expanded: boolean) => {
  isExpanded.value = expanded;
};

const onTrigger = () => {
  if (isExpanded.value) {
    multiSelection.resetSelection();
  }
  toggleExpansion();
};

defineExpose({
  toggleExpansion,
  setExpanded,
  isExpanded: () => isExpanded.value,
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

  const draggedItem = props.inputOutputItem.subItems?.[index];
  if (!draggedItem || !draggedItem.supported) {
    return;
  }

  const subItemNode = event.currentTarget;
  let dragGhostContent;
  if (
    subItemNode instanceof HTMLElement &&
    subItemNode.getElementsByClassName("sub-item-icon-name-wrapper")[0]
  ) {
    const wrapperElement = subItemNode
      .getElementsByClassName("sub-item-icon-name-wrapper")[0]
      .cloneNode(true) as HTMLElement;
    wrapperElement.classList.remove("sub-item-icon-name-wrapper");
    dragGhostContent = wrapperElement;
  } else {
    dragGhostContent = document.createElement("div");
    dragGhostContent.textContent = draggedItem.name;
  }

  const dragGhost = createDragGhost({
    elements: [{ dragGhostContent }],
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
  const el = document.createElement("div");
  el.innerText = props.inputOutputItem.codeAlias!;
  const dragGhost = createDragGhost({
    elements: [{ dragGhostContent: el }],
    numSelectedItems: 1,
    font: "var(--kds-font-base-code-xsmall)",
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

const fallbackType = computed(() => {
  if (props.inputOutputItem.portType === "table") {
    return { id: "unknown-datatype", text: "Unknown data type" };
  }
  if (props.inputOutputItem.portType === "flowVariable") {
    return { id: "UNKNOWN", text: "Unknown variable type" };
  }
  return null;
});
</script>

<template>
  <div v-if="props.inputOutputItem.subItems?.length" class="collapser">
    <BaseButton
      class="button"
      :aria-expanded="String(isExpanded)"
      @click.prevent="onTrigger"
    >
      <div class="top-card">
        <div class="title">
          <div class="port-icon-container">
            <KdsIcon
              v-if="inputOutputItem.portType === 'view'"
              name="eye"
              size="small"
            />
            <svg v-else viewBox="-6 -6 12 12">
              <PortIcon
                :type="inputOutputItem.portType"
                :color="inputOutputItem.portIconColor"
              />
            </svg>
          </div>
          {{ inputOutputItem.name }}
        </div>
        <div class="header-actions">
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
          <KdsIcon
            name="chevron-down"
            size="xsmall"
            :class="{ 'dropdown-icon': true, 'rotate-icon': isExpanded }"
          />
        </div>
      </div>
    </BaseButton>
    <transition name="collapser-expand">
      <div v-show="isExpanded" class="collapser-content">
        <div
          v-for="(subItem, index) in props.inputOutputItem.subItems"
          :key="index"
          class="sub-item"
          :class="{
            disabled: !subItem.supported || globalReadOnly,
          }"
        >
          <div
            class="sub-item-content"
            :class="{
              draggable:
                inputOutputItem.subItemCodeAliasTemplate &&
                !globalReadOnly &&
                subItem.supported,
              interactive: props.inputOutputItem.subItemCodeAliasTemplate,
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
            <div class="sub-item-left">
              <div
                class="sub-item-icon-name-wrapper"
                :class="{
                  selected:
                    props.inputOutputItem.subItemCodeAliasTemplate &&
                    props.inputOutputItem.multiSelection &&
                    multiSelection.isSelected(index) &&
                    subItem.supported,
                }"
              >
                <KdsDataType
                  v-if="subItem.type.id"
                  size="small"
                  class="sub-item-icon"
                  :icon-name="subItem.type.id"
                  :icon-title="subItem.type.title"
                />
                <KdsDataType
                  v-else-if="
                    subItem.type.displayName === 'UNKNOWN' && fallbackType
                  "
                  size="small"
                  :icon-name="fallbackType.id"
                  :icon-title="fallbackType.text"
                />
                <span class="sub-item-name">
                  {{ subItem.name }}
                </span>
              </div>
              <div
                v-if="typeof subItem.icon !== 'undefined'"
                class="sub-item-component"
              >
                <component
                  :is="subItem.icon.component"
                  v-bind="subItem.icon.props"
                />
              </div>
            </div>
            <span class="sub-item-type">
              {{ subItem.type.displayName }}
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
  <div v-else class="top-card">
    <div class="title">
      <div class="port-icon-container">
        <KdsIcon
          v-if="inputOutputItem.portType === 'view'"
          name="eye"
          size="small"
        />
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
.button {
  position: relative;
  width: 100%;
  padding: 0;
  font-size: 18px;
  font-weight: bold;
  line-height: 26px;
  color: inherit; /* Safari needs this */
  text-align: left;
  appearance: none;
  cursor: pointer;
  outline: none;
  background-color: transparent;
  border: 0;

  .dropdown-icon {
    height: var(--kds-dimension-icon-0-56x);
    width: var(--kds-dimension-icon-0-56x);
  }

  .rotate-icon {
    transform: rotate(180deg);
  }

  & :deep(.dropdown-icon) {
    color: var(--kds-color-text-and-icon-neutral);
  }

  & :deep(.dropdown-icon svg) {
    fill: var(--kds-color-text-and-icon-neutral);
  }
}

.top-card {
  --in-out-item-icon-size: 12px;

  height: var(--kds-dimension-component-height-1-5x);
  background-color: var(--kds-color-surface-muted);
  padding-left: var(--kds-spacing-container-0-37x);
  padding-right: var(--kds-spacing-container-0-37x);
  border-radius: var(--kds-border-radius-container-0-25x);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.port-icon-container {
  width: var(--in-out-item-icon-size);
  min-width: var(--in-out-item-icon-size);
  height: var(--in-out-item-icon-size);
  display: inline-block;
  overflow: hidden;
  position: relative;

  svg {
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
  }
}

.sub-item-left {
  display: flex;
  align-items: center;
  gap: var(--kds-spacing-container-0-12x);
}

.sub-item-component {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.sub-item-icon {
  height: var(--kds-dimension-component-height-0-75x);
  width: var(--kds-dimension-component-height-0-75x);
  gap: var(--kds-spacing-container-none);
  color: var(--kds-color-text-and-icon-muted);
  border: var(--kds-border-base-muted);
  border-radius: var(--kds-border-radius-container-0-12x);
  border-width: var(--kds-border-width-icon-stroke-s);
}

.title {
  color: var(--kds-color-text-and-icon-neutral);
  font: var(--kds-font-base-title-small-strong);
  gap: var(--kds-spacing-container-0-25x);
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--kds-spacing-container-0-25x);
  min-width: 0;
}

.collapser-content {
  font-size: 11px;
  width: 100%;
  padding-top: 1px;
}

.sub-item {
  display: flex;
  font: var(--kds-font-base-interactive-small);
  color: var(--kds-color-text-and-icon-neutral);
  height: var(--kds-dimension-component-height-1-5x);
  padding-left: var(--kds-spacing-container-0-25x);
  padding-right: var(--kds-spacing-container-0-25x);
  border-radius: var(--kds-border-radius-container-0-31xs);

  &.disabled {
    color: var(--kds-color-text-and-icon-disabled);
    pointer-events: none;
  }
}

.sub-item-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding-left: var(--kds-spacing-container-0-25x);
  padding-right: var(--kds-spacing-container-0-25x);
  width: 100%;

  &.draggable {
    cursor: grab;
  }
}

.interactive {
  text-wrap: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sub-item-icon-name-wrapper {
  display: flex;
  align-items: center;
  height: var(--kds-dimension-component-height-1-25x);
  padding-right: var(--kds-spacing-container-0-25x);
  padding-left: var(--kds-spacing-container-0-25x);
  gap: var(--kds-spacing-container-0-12x);
  background-color: var(--kds-color-background-neutral-initial);
  border-radius: var(--kds-border-radius-container-0-31x);

  &.selected {
    background-color: var(--kds-color-background-selected-initial);
    border-radius: var(--kds-border-radius-container-0-31x);
    color: var(--kds-color-text-and-icon-selected);
  }

  .sub-item-name {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.sub-item-content:hover .sub-item-icon-name-wrapper:not(.selected) {
  background-color: var(--kds-color-background-neutral-hover);
  border-radius: var(--kds-border-radius-container-0-31x);
}

.sub-item-type {
  text-align: end;
  fill: var(--kds-color-text-and-icon-neutral);
  font: var(--kds-font-base-interactive-small-italic);
}

.code-alias {
  font: var(--kds-font-base-code-xsmall);
  color: var(--kds-color-text-and-icon-neutral);
  height: var(--kds-dimension-component-height-1x);
  padding-left: var(--kds-spacing-container-0-25x);
  padding-right: var(--kds-spacing-container-0-25x);
  border-radius: var(--kds-border-radius-container-0-31x);
  cursor: grab;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1 1 auto;

  &:active {
    cursor: grabbing;
  }

  &:hover {
    background-color: var(--kds-color-background-neutral-hover);
  }
}

.code-alias-dragging {
  background-color: var(--kds-color-background-selected-initial);
  color: var(--kds-color-text-and-icon-selected);
  font: var(--kds-font-base-code-xsmall);
}
</style>
