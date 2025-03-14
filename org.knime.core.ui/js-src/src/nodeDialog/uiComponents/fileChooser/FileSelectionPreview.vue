<script setup lang="ts">
import { computed, ref } from "vue";

import { FunctionButton } from "@knime/components";
import FileIcon from "@knime/styles/img/icons/file.svg";

type PropType = {
  items: string[];
  expandByDefault?: boolean;
};

const props = withDefaults(defineProps<PropType>(), {
  expandByDefault: false,
});

const allItems = computed(() => props.items);

/**
 * All items that pass the filters
 */
const visibleItems = computed(
  () => allItems.value, // TODO implement filtering
);

const headerText = computed(() => {
  if (allItems.value.length === visibleItems.value.length) {
    return `${visibleItems.value.length} files`;
  } else {
    return `${visibleItems.value.length} of ${allItems.value.length} files`;
  }
});

const showAll = ref(props.expandByDefault);

const showAllButtonClickHandler = () => {
  showAll.value = !showAll.value;
};
</script>

<template>
  <div class="container">
    <div class="visible-items-header">
      <div class="header-icon"><FileIcon /></div>
      <span class="header-text">{{ headerText }}</span>
      <slot name="header-buttons-right" />
    </div>
    <div v-if="showAll" class="visible-items-container">
      <div v-for="item in visibleItems" class="visible-item">
        {{ item }}
      </div>
    </div>
    <div class="visible-items-footer">
      <FunctionButton class="filter-button" @click="showAllButtonClickHandler">
        {{ showAll ? "Hide all" : "Show all" }}
      </FunctionButton>
    </div>
  </div>
</template>

<style scoped lang="postcss">
.container {
  display: flex;
  flex-direction: column;
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
}

.visible-items-container {
  --visible-item-height: 26px;

  display: flex;
  flex-direction: column;
  padding: var(--space-4) 0;
  background: white;
  overflow: hidden auto;
  width: 100%;
  max-height: calc(8 * var(--visible-item-height));

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
</style>
