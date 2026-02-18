<script lang="ts" setup>
import { computed, ref } from "vue";

import { SideDrawer, SplitPanel } from "@knime/components";
import { Porcelain } from "@knime/styles/colors/knimeColors";

import SidePanelBackArrow from "./SidePanelBackArrow.vue";

const props = defineProps<{
  isLargeMode: boolean;
}>();

const emit = defineEmits<{
  refocusTable: [];
}>();

const rightPaneExpanded = ref(true);
const rightPaneSize = ref<number>(300);

const isExpanded = ref(false);
const expand = () => {
  isExpanded.value = true;
};
const close = () => {
  isExpanded.value = false;
  emit("refocusTable");
};

defineExpose({
  showPanelContent: () => {
    if (props.isLargeMode) {
      rightPaneExpanded.value = true;
    } else {
      expand();
    }
  },
});

const styleOverrides = computed(() => ({
  width: "100%",
  position: "absolute" as const,
  backgroundColor: Porcelain,
}));
</script>

<template>
  <SplitPanel
    v-if="isLargeMode"
    v-model:expanded="rightPaneExpanded"
    v-model:secondary-size="rightPaneSize"
    direction="right"
    :secondary-snap-size="220"
    use-pixel
    keep-element-on-close
  >
    <slot name="main-table" />
    <template #secondary>
      <slot name="side-panel" :close="close" />
    </template>
  </SplitPanel>
  <div v-else class="small-mode-container">
    <slot name="main-table" />
    <SideDrawer :is-expanded class="side-drawer" :style-overrides>
      <div class="side-drawer-content">
        <SidePanelBackArrow back-button-label="Back to table" @click="close" />
        <slot name="side-panel" :close="close" />
      </div>
    </SideDrawer>
  </div>
</template>

<style scoped lang="postcss">
.small-mode-container {
  position: relative;
  height: 100%;
  width: 100%;

  /**
   * The settings subpanel does overflow for animation reasons
  */
  overflow-x: hidden;
}

.side-drawer-content {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  width: 100%;

  & .main-content {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
  }
}
</style>
