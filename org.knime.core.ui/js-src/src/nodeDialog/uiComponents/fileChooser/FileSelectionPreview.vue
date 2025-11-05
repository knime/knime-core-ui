<script setup lang="ts">
import { computed, ref } from "vue";

import { FunctionButton, LoadingIcon } from "@knime/components";
import ErrorIcon from "@knime/styles/img/icons/circle-close.svg";
import FileIcon from "@knime/styles/img/icons/file.svg";

import type { MultiFileFilterMode } from "@/nodeDialog/types/FileChooserUiSchema";

import type { PreviewResult } from "./composables/useFileFilterPreviewBackend";

type PropType = {
  previewData: PreviewResult;
  filterMode: MultiFileFilterMode;
  expandByDefault?: boolean;
  isLoading?: boolean;
};

const props = withDefaults(defineProps<PropType>(), {
  expandByDefault: false,
  isLoading: false,
});

const allItems = computed<string[]>(() =>
  props.previewData.resultType === "SUCCESS"
    ? props.previewData.itemsAfterFiltering
    : [],
);

const itemDescription = computed<string>(() => {
  switch (props.filterMode) {
    case "FILES_AND_FOLDERS":
      return "files/folders";
    case "FOLDERS":
      return "folders";
    default:
      return "files";
  }
});

const headerText = computed(() => {
  if (props.previewData.resultType === "ERROR") {
    return `Error loading ${itemDescription.value}`;
  }

  const numerator = allItems.value.length;
  const denominator = props.previewData.numItemsBeforeFiltering;

  const numeratorText = `${numerator}${
    props.previewData.numFilesAfterFilteringIsOnlyLowerBound ? "+" : ""
  }`;
  const denominatorText = `${denominator}${
    props.previewData.numFilesBeforeFilteringIsOnlyLowerBound ? "+" : ""
  }`;

  return `${numeratorText} of ${denominatorText} ${itemDescription.value}`;
});

const showAll = ref(props.expandByDefault);

const showAllButtonClickHandler = () => {
  showAll.value = !showAll.value;
};
</script>

<template>
  <div class="container">
    <div class="everything-except-error-message">
      <div
        class="visible-items-header"
        :class="{ error: previewData.resultType === 'ERROR' }"
      >
        <div class="header-icon">
          <FileIcon v-if="previewData.resultType === 'SUCCESS'" />
          <ErrorIcon v-else />
        </div>
        <span class="header-text">{{ headerText }}</span>
        <slot name="header-buttons-right" />
      </div>
      <div v-if="showAll" class="visible-items-container">
        <!-- This overlays everything else when it's enabled -->
        <div v-if="props.isLoading" class="loading-overlay">
          <LoadingIcon class="loading-spinner" />
        </div>
        <template v-if="allItems.length === 0">
          <div class="empty-list">No files</div>
        </template>
        <template v-else>
          <div
            v-for="item in allItems"
            :key="item"
            :title="item"
            class="visible-item"
          >
            {{ item }}
          </div>
        </template>
      </div>
      <div class="visible-items-footer">
        <FunctionButton
          class="filter-button"
          @click="showAllButtonClickHandler"
        >
          {{ showAll ? "Hide all" : "Show all" }}
        </FunctionButton>
      </div>
    </div>
    <template v-if="previewData.resultType === 'ERROR'">
      <div
        class="flexible-error-message-container"
        data-test-id="error-message"
        :title="previewData.errorMessage"
      >
        {{ previewData.errorMessage }}
      </div>
    </template>
  </div>
</template>

<style scoped lang="postcss">
.container {
  display: flex;
  flex-direction: column;
  position: relative;
  margin: 2px 0;
}

.everything-except-error-message {
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
}

.filter-button {
  display: flex;
  text-wrap: nowrap;
  flex-grow: 0;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  font-family: Roboto, sans-serif;
  color: var(--color-primary);
  cursor: pointer;

  & svg {
    width: 16px;
    height: 16px;
  }

  &.show-mode {
    color: var(--knime-coral-dark);
  }
}

.visible-items-header,
.visible-items-footer {
  display: flex;
  flex-flow: row nowrap;
  height: 40px;
  align-items: center;
  padding: var(--space-8);
  gap: var(--space-8);
  background-color: white;
}

.visible-items-header {
  border-bottom: 1px solid var(--knime-porcelain);

  & .header-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-grow: 0;
    height: 100%;

    & svg {
      width: 24px;
      height: 24px;
    }
  }

  & .header-text {
    flex-grow: 1;
    align-items: center;
    font-size: 13px;
    font-weight: 500;
    font-family: Roboto, sans-serif;
    text-overflow: ellipsis;
    text-wrap: nowrap;
    white-space: nowrap;
  }

  &.error {
    & .header-text {
      color: var(--knime-coral-dark);
    }

    & .header-icon svg {
      color: var(--knime-coral-dark);
      stroke: var(--knime-coral-dark);
    }
  }
}

.visible-items-container {
  --visible-item-height: 26px;
  --max-visible-items: 8;

  position: relative;
  display: flex;
  flex-direction: column;
  padding: var(--space-4) 0;
  background: white;
  overflow: hidden auto;
  width: 100%;
  height: calc(var(--max-visible-items) * var(--visible-item-height));

  & .loading-overlay {
    position: absolute;
    inset: 0;
    background-color: white;
    opacity: 0.5;
    pointer-events: fill;

    & .loading-spinner {
      --spinner-size: 32px;

      position: absolute;
      top: calc(50% - var(--spinner-size) / 2);
      left: calc(50% - var(--spinner-size) / 2);
      height: var(--spinner-size);
      width: var(--spinner-size);
    }

    &.error {
      color: var(--knime-coral-dark);

      & span {
        position: absolute;
        top: 50%;
        left: 50%;
        font-size: 14px;
        font-weight: 500;
        font-family: Roboto, sans-serif;
      }
    }
  }

  & .visible-item {
    padding: 0 var(--space-8);
    font-size: 14px;
    min-height: var(--visible-item-height);
    height: var(--visible-item-height);
    line-height: var(--visible-item-height);
    width: 100%;
    font-weight: 300;
    text-overflow: ellipsis;
    text-wrap: nowrap;
    white-space: nowrap;
    overflow: hidden;
    font-family: Roboto, sans-serif;
  }

  & .empty-list {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 14px;
    font-weight: 300;
    font-family: Roboto, sans-serif;
    font-style: italic;
  }
}

.visible-items-footer {
  border-top: 1px solid var(--knime-porcelain);
  justify-content: center;

  & .function-button {
    display: flex;
    text-wrap: nowrap;
    align-items: center;
    font-size: 13px;
    font-weight: 500;
    font-family: Roboto, sans-serif;
    color: var(--color-primary);
    cursor: pointer;
  }
}

.flexible-error-message-container {
  /* two lines, ellipsis on overflow */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;

  /* other styling for this element */
  margin-top: var(--space-8);
  color: var(--knime-coral-dark);
  font-size: 10px;
  line-height: 12px;
  font-weight: 500;
  font-family: Roboto, sans-serif;
  min-height: 12px;
  width: 100%;
}
</style>
