<script setup lang="ts">
import { onKeyStroke } from "@vueuse/core";
import { Pane, Splitpanes } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import { computed, onMounted, reactive, ref } from "vue";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";

import { useMainCodeEditor } from "@/editor";
import {
  getScriptingService,
  initConsoleEventHandler,
} from "@/scripting-service";
import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import CompactTabBar from "./CompactTabBar.vue";
import FooterBar from "./FooterBar.vue";
import HeaderBar from "./HeaderBar.vue";
import InputOutputPane from "./InputOutputPane.vue";
import type { ConsoleHandler } from "./OutputConsole.vue";
import OutputConsole from "./OutputConsole.vue";
import type { SettingsMenuItem } from "./SettingsPage.vue";
import SettingsPage from "./SettingsPage.vue";
import { consoleHandlerStore } from "@/consoleHandler";

export type PaneSizes = {
  [key in "left" | "right" | "bottom"]: number;
};

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
});

const emit = defineEmits(["menu-item-clicked", "save-settings"]);

// Splitpane sizes
const currentPaneSizes = reactive<PaneSizes>({
  left: props.initialPaneSizes.left,
  right: props.initialPaneSizes.right,
  bottom: props.initialPaneSizes.bottom,
});
const previousPaneSizes = reactive<PaneSizes>({
  left: props.initialPaneSizes.left,
  right: props.initialPaneSizes.right,
  bottom: props.initialPaneSizes.bottom,
});
const usedMainPaneSize = computed(() => 100 - currentPaneSizes.left);
const usedHorizontalCodeEditorPaneSize = computed(
  () => 100 - currentPaneSizes.right,
);
const usedVerticalCodeEditorPaneSize = computed(
  () => 100 - currentPaneSizes.bottom,
);
const isLeftPaneCollapsed = computed(() => currentPaneSizes.left === 0);
const isRightPaneCollapsed = computed(() => currentPaneSizes.right === 0);
const isBottomPaneCollapsed = computed(() => currentPaneSizes.bottom === 0);
const updatePreviousPaneSize = (pane: keyof PaneSizes) => {
  if (currentPaneSizes[pane] <= 0) {
    return;
  }
  previousPaneSizes[pane] = currentPaneSizes[pane];
};
const resizePane = (
  size: number,
  pane: keyof PaneSizes,
  shouldUpdatePreviousPaneSize: boolean = true,
) => {
  currentPaneSizes[pane] = size;

  if (shouldUpdatePreviousPaneSize) {
    updatePreviousPaneSize(pane);
  }
};
const updateRightPane = (size: number) => {
  if (props.rightPaneLayout !== "fixed" || currentPaneSizes.right <= 0) {
    return;
  }
  // keep right pane at same size when left pane is resized
  const newMainPaneSize = 100 - size;
  const absoluteRightPaneSize =
    (100 - currentPaneSizes.left) * (previousPaneSizes.right / 100);
  const newRightPaneSize = (absoluteRightPaneSize / newMainPaneSize) * 100;

  resizePane(newRightPaneSize, "right", false);
};
const collapsePane = (pane: keyof PaneSizes) => {
  let newSize = currentPaneSizes[pane] === 0 ? previousPaneSizes[pane] : 0;
  if (pane === "left") {
    updateRightPane(newSize);
  }
  resizePane(newSize, pane);
};

// Main editor
const editorContainer = ref<HTMLDivElement>();
const codeEditorState = useMainCodeEditor({
  container: editorContainer,
  language: props.language,
  fileName: props.fileName,
});
onMounted(() => {
  getScriptingService()
    .getInitialSettings()
    .then((settings) => {
      codeEditorState.setInitialText(settings.script);
    });
});
// register undo changes from outside the editor
onKeyStroke("z", (e) => {
  const key = navigator.userAgent.toLowerCase().includes("mac")
    ? e.metaKey
    : e.ctrlKey;
  if (key) {
    codeEditorState.editor.value?.trigger("window", "undo", {});
  }
});
// Dropping input/output items
const dropEventHandler = ref<(payload: DragEvent) => void>();
const onDropEventHandlerCreated = (handler: (payload: DragEvent) => void) => {
  dropEventHandler.value = handler;
};

// Saving and closing
const saveSettings = () => {
  const settings = { script: codeEditorState.text.value };
  emit("save-settings", settings);
};
const closeDialog = () => getScriptingService().closeDialog();

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
  consoleHandlerStore.value = handler;
  initConsoleEventHandler();
};
</script>

<template>
  <div class="layout">
    <HeaderBar
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
      data-testid="mainSplitpane"
      class="common-splitter unset-transition main-splitpane"
      :dbl-click-splitter="false"
      :class="{
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
                <div
                  ref="editorContainer"
                  class="code-editor"
                  @drop="dropEventHandler"
                />
                <CodeEditorControlBar
                  v-if="showControlBar"
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
    <FooterBar
      v-show="!showSettingsPage"
      @scripting-editor-okayed="saveSettings"
      @scripting-editor-cancelled="closeDialog"
    />
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

.code-editor {
  height: calc(100% - var(--controls-height));
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
  height: calc(100vh - (2 * var(--controls-height)));
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
.settings-page {
  height: calc(100vh - var(--controls-height));
}

.right-pane {
  background-color: var(--knime-gray-ultra-light);
}

.scrollable-y {
  overflow-y: auto;
}
</style>
@/consoleHandler
