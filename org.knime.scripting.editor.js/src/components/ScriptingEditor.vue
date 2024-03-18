<script setup lang="ts">
import { useElementBounding } from "@vueuse/core";

import { computed, reactive, ref } from "vue";
import { Pane, Splitpanes } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";
import {
  initConsoleEventHandler,
  type NodeSettings,
} from "@/scripting-service";
import CompactTabBar from "./CompactTabBar.vue";
import HeaderBar from "./HeaderBar.vue";
import InputOutputPane from "./InputOutputPane.vue";
import type { ConsoleHandler } from "./OutputConsole.vue";
import OutputConsole from "./OutputConsole.vue";
import type { SettingsMenuItem } from "./SettingsPage.vue";
import SettingsPage from "./SettingsPage.vue";
import { setConsoleHandler } from "@/consoleHandler";
import MainEditorPanel from "@/components/MainEditorPanel.vue";
import {
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  type PaneSizes,
} from "@/components/utils/paneSizes";
import CodeEditorControlBar from "@/components/CodeEditorControlBar.vue";

const commonMenuItems: MenuItem[] = [
  // TODO: add actual common menu items
];

// Props
interface Props {
  title: string;
  language: string;
  fileName: string;
  rightPaneLayout?: "fixed" | "relative";
  menuItems?: MenuItem[];
  showControlBar?: boolean;
  initialPaneSizes?: PaneSizes;
  toSettings?: (settings: NodeSettings) => NodeSettings;
}

const props = withDefaults(defineProps<Props>(), {
  rightPaneLayout: "fixed",
  menuItems: () => [],
  showControlBar: true,
  initialPaneSizes: () => ({
    left: 20,
    right: 25,
    bottom: 30,
  }),
  toSettings: (settings: NodeSettings) => settings,
});

// Splitpane sizes
const largeModePaneSizes = reactive<PaneSizes>({
  left: props.initialPaneSizes.left,
  right: props.initialPaneSizes.right,
  bottom: props.initialPaneSizes.bottom,
});
const previousLargeModePaneSizes = reactive<PaneSizes>({
  left: props.initialPaneSizes.left,
  right: props.initialPaneSizes.right,
  bottom: props.initialPaneSizes.bottom,
});

const rootSplitPane = ref();
const rootSplitPaneRef = useElementBounding(rootSplitPane);
const isSlimMode = computed(
  () => rootSplitPaneRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_PANES,
);
const currentPaneSizes = computed(() => {
  return isSlimMode.value
    ? {
        left: 0,
        right: 0,
        bottom: 0,
      }
    : largeModePaneSizes;
});

const usedMainPaneSize = computed(() => 100 - currentPaneSizes.value.left);
const usedHorizontalCodeEditorPaneSize = computed(
  () => 100 - currentPaneSizes.value.right,
);
const usedVerticalCodeEditorPaneSize = computed(
  () => 100 - currentPaneSizes.value.bottom,
);
const isLeftPaneCollapsed = computed(() => currentPaneSizes.value.left === 0);
const isRightPaneCollapsed = computed(() => currentPaneSizes.value.right === 0);
const isBottomPaneCollapsed = computed(
  () => currentPaneSizes.value.bottom === 0,
);
const updatePreviousPaneSize = (pane: keyof PaneSizes) => {
  if (currentPaneSizes.value[pane] <= 0) {
    return;
  }
  previousLargeModePaneSizes[pane] = largeModePaneSizes[pane];
};
const resizePane = (
  size: number,
  pane: keyof PaneSizes,
  shouldUpdatePreviousPaneSize: boolean = true,
) => {
  currentPaneSizes.value[pane] = size;

  if (shouldUpdatePreviousPaneSize) {
    updatePreviousPaneSize(pane);
  }
};
const updateRightPane = (size: number) => {
  if (props.rightPaneLayout !== "fixed" || currentPaneSizes.value.right <= 0) {
    return;
  }
  // keep right pane at same size when left pane is resized
  const newMainPaneSize = 100 - size;
  const absoluteRightPaneSize =
    (100 - currentPaneSizes.value.left) *
    (previousLargeModePaneSizes.right / 100);
  const newRightPaneSize = (absoluteRightPaneSize / newMainPaneSize) * 100;

  resizePane(newRightPaneSize, "right", false);
};
const collapsePane = (pane: keyof PaneSizes) => {
  let newSize =
    currentPaneSizes.value[pane] === 0 ? previousLargeModePaneSizes[pane] : 0;
  if (pane === "left") {
    updateRightPane(newSize);
  }
  resizePane(newSize, pane);
};

// Dropping input/output items
const dropEventHandler = ref<(payload: DragEvent) => void>();
const onDropEventHandlerCreated = (handler: (payload: DragEvent) => void) => {
  dropEventHandler.value = handler;
};

const emit = defineEmits(["menu-item-clicked"]);

// Menu items and settings pane
const showSettingsPage = ref(false);
const onMenuItemClicked = (args: { event: Event; item: SettingsMenuItem }) => {
  showSettingsPage.value = Boolean(args.item.showSettingsPage);

  // TODO: handle click actions for common items here instead of calling the emit
  emit("menu-item-clicked", args);
};

// Bottom pane tabs
const bottomPaneOptions = [{ value: "console", label: "Console" }];
const bottomPaneActiveTab = ref<"console">("console");
const onConsoleCreated = (handler: ConsoleHandler) => {
  setConsoleHandler(handler);
  initConsoleEventHandler();
};

// SplitPanes does interfere with the usage of flex box so the following line
// is a workaround to allow to hide the control bar
const controlBarHeight = computed(() => {
  return props.showControlBar && !isSlimMode.value ? "40px" : "0px";
});
</script>

<template>
  <div class="layout" :style="{ '--controls-height': controlBarHeight }">
    <HeaderBar
      v-if="!isSlimMode"
      :title="title"
      :menu-items="[...commonMenuItems, ...menuItems]"
      @menu-item-click="onMenuItemClicked"
    />

    <SettingsPage
      v-if="showSettingsPage"
      @close-settings-page="showSettingsPage = false"
    >
      <template #settings-title>
        <slot name="settings-title" />
      </template>
      <template #settings-content>
        <slot name="settings-content" />
      </template>
    </SettingsPage>

    <splitpanes
      v-show="!showSettingsPage"
      ref="rootSplitPane"
      data-testid="mainSplitpane"
      class="common-splitter unset-transition main-splitpane"
      :dbl-click-splitter="false"
      :class="{
        'slim-mode': isSlimMode,
        'left-facing-splitter': !isLeftPaneCollapsed,
        'right-facing-splitter': isLeftPaneCollapsed,
      }"
      @splitter-click="
        collapsePane('left');
        updatePreviousPaneSize('right');
      "
      @resize="
        updateRightPane($event[0].size);
        resizePane($event[0].size, 'left', false);
        updatePreviousPaneSize('right');
      "
      @resized="updatePreviousPaneSize('left')"
    >
      <pane
        data-testid="leftPane"
        :size="currentPaneSizes.left"
        class="scrollable-y"
      >
        <InputOutputPane
          @drop-event-handler-created="onDropEventHandlerCreated"
        />
      </pane>

      <pane data-testid="mainPane" :size="usedMainPaneSize" min-size="40">
        <splitpanes
          data-testid="horizontalSplitpane"
          horizontal
          class="common-splitter horizontal-splitpane"
          :dbl-click-splitter="false"
          :class="{
            'down-facing-splitter': !isBottomPaneCollapsed,
            'up-facing-splitter': isBottomPaneCollapsed,
          }"
          @splitter-click="collapsePane('bottom')"
          @resize="resizePane($event[1].size, 'bottom')"
        >
          <pane
            data-testid="topPane"
            :size="usedVerticalCodeEditorPaneSize"
            min-size="40"
          >
            <splitpanes
              data-testid="verticalSplitpane"
              class="common-splitter unset-transition vertical-splitpane"
              :class="{
                'left-facing-splitter': isRightPaneCollapsed,
                'right-facing-splitter': !isRightPaneCollapsed,
              }"
              :dbl-click-splitter="false"
              @splitter-click="collapsePane('right')"
              @resized="resizePane($event[1].size, 'right')"
            >
              <pane
                data-testid="editorPane"
                :size="usedHorizontalCodeEditorPaneSize"
                min-size="25"
              >
                <MainEditorPanel
                  :language="props.language"
                  :file-name="props.fileName"
                  :drop-event-handler="dropEventHandler"
                  :to-settings="props.toSettings"
                  class="main-editor-panel"
                />
                <CodeEditorControlBar
                  v-if="showControlBar && !isSlimMode"
                  :language="language"
                  :current-pane-sizes="currentPaneSizes"
                >
                  <template #controls>
                    <slot name="code-editor-controls" />
                  </template>
                </CodeEditorControlBar>
              </pane>
              <pane
                data-testid="rightPane"
                :size="currentPaneSizes.right"
                class="right-pane"
              >
                <slot name="right-pane" />
              </pane>
            </splitpanes>
          </pane>
          <pane data-testid="bottomPane" :size="currentPaneSizes.bottom">
            <div class="tab-bar-container">
              <CompactTabBar
                v-model="bottomPaneActiveTab"
                class="scripting-editor-tab-bar"
                :possible-values="bottomPaneOptions"
              />
              <div class="console-container">
                <OutputConsole
                  v-show="bottomPaneActiveTab === 'console'"
                  @console-created="onConsoleCreated"
                >
                  <template #console-status>
                    <slot name="console-status" />
                  </template>
                </OutputConsole>
              </div>
            </div>
          </pane>
        </splitpanes>
      </pane>
    </splitpanes>
  </div>
</template>

<style lang="postcss" scoped>
.layout {
  --controls-height: 40px;
  --description-button-size: 15px;

  display: flex;
  flex-direction: column;
  height: calc(100vh);
  width: 100%;
  flex-grow: 0;
  overflow: hidden;
  position: relative;
}

.tab-bar-container {
  display: flex;
  height: 100%;
  flex-direction: column;
  padding-left: 10px;
  padding-right: 10px;
  position: relative;

  & .console-container {
    flex: 1;
    min-height: 0;
  }
}

.main-editor-panel {
  height: calc(100% - var(--controls-height));
}

/* NB: we disable the rule because of classes defined by the splitpanes package */
/* stylelint-disable selector-class-pattern */
.common-splitter {
  & :deep(.splitpanes__splitter) {
    min-width: 11px;
    min-height: 11px;
    background-color: var(--knime-porcelain);
    background-repeat: no-repeat;
    background-position: center;
    border-color: var(--knime-silver-sand);
    border-style: solid;
  }
}

.common-splitter.slim-mode {
  & :deep(.splitpanes__splitter) {
    display: none;
    pointer-events: none;
  }
}

.splitpanes--vertical {
  & :deep(> .splitpanes__splitter) {
    border-width: 0 1px;
  }
}

.splitpanes--horizontal {
  & :deep(> .splitpanes__splitter) {
    border-width: 1px 0;
  }
}

.left-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("../../webapps-common/ui/assets/img/icons/arrow-prev.svg");
  }
}

.right-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("../../webapps-common/ui/assets/img/icons/arrow-next.svg");
  }
}

.down-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("../../webapps-common/ui/assets/img/icons/arrow-dropdown.svg");
  }
}

.up-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("../../webapps-common/ui/assets/img/icons/arrow-dropdown.svg");
    transform: scaleY(-1);
  }
}

.splitpanes__pane {
  transition: unset;
}

.main-splitpane {
  overflow: hidden;

  &:deep(> .splitpanes__splitter) {
    &:hover {
      border-right: 1px solid var(--knime-masala);
    }
  }
}

.horizontal-splitpane {
  &:deep(> .splitpanes__splitter) {
    &:hover {
      border-top: 1px solid var(--knime-masala);
    }
  }
}

.vertical-splitpane {
  &:deep(> .splitpanes__splitter) {
    &:hover {
      border-left: 1px solid var(--knime-masala);
    }
  }
}

/* stylelint-enable selector-class-pattern */
.right-pane {
  background-color: var(--knime-gray-ultra-light);
}

.scrollable-y {
  overflow-y: auto;
}
</style>
