<script setup lang="ts">
import { useElementBounding } from "@vueuse/core";
import { computed, ref, useSlots } from "vue";
import { Pane, Splitpanes } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import type { MenuItem } from "@knime/components";
import HeaderBar from "./HeaderBar.vue";
import InputOutputPane from "./InputOutputPane.vue";
import type { SettingsMenuItem } from "./SettingsPage.vue";
import SettingsPage from "./SettingsPage.vue";
import MainEditorPane from "./MainEditorPane.vue";
import { type PaneSizes } from "@/components/utils/paneSizes";
import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import { useResizeLogic } from "@/components/utils/resizeLogic";
import ScriptingEditorBottomPane from "./ScriptingEditorBottomPane.vue";
import { type GenericNodeSettings } from "@/settings-service";

const commonMenuItems: MenuItem[] = [
  // TODO: add actual common menu items
];

type TabItem = {
  label: string;
  value: string;
};

// Props
interface Props {
  title?: string | null;
  language: string;
  fileName?: string | null;
  rightPaneLayout?: "fixed" | "relative";
  menuItems?: MenuItem[];
  showControlBar?: boolean;
  initialPaneSizes?: PaneSizes;
  rightPaneMinimumWidthInPixel?: number;
  toSettings?: (settings: GenericNodeSettings) => GenericNodeSettings;
  additionalBottomPaneTabContent?: TabItem[];
}

const props = withDefaults(defineProps<Props>(), {
  title: null,
  fileName: null,
  rightPaneLayout: "fixed",
  menuItems: () => [],
  showControlBar: true,
  initialPaneSizes: () => ({
    left: 20,
    right: 25,
    bottom: 30,
  }),
  rightPaneMinimumWidthInPixel: () => 0,
  additionalBottomPaneTabContent: () => [] as TabItem[],
  toSettings: (settings: GenericNodeSettings) => settings,
});

const isRightPaneCollapsable = computed(
  () => props.rightPaneMinimumWidthInPixel === 0,
);
const emit = defineEmits(["menu-item-clicked"]);

const rootSplitPane = ref();
const rootSplitPaneRef = useElementBounding(rootSplitPane);
const editorSplitPane = ref();
const editorSplitPaneRef = useElementBounding(editorSplitPane);

// All the logic for resizing panes
const {
  collapseAllPanes,
  collapsePane,
  collapseLeftPane,
  currentPaneSizes,
  isBottomPaneCollapsed,
  isLeftPaneCollapsed,
  isRightPaneCollapsed,
  minRatioOfRightPaneInPercent,
  resizePane,
  updatePreviousPaneSize,
  updateRightPane,
  showButtonText,
  usedHorizontalCodeEditorPaneSize,
  usedMainPaneSize,
  usedVerticalCodeEditorPaneSize,
} = useResizeLogic({
  initialPaneSizes: props.initialPaneSizes,
  rightPaneMinimumWidthInPixel: props.rightPaneMinimumWidthInPixel,
  rightPaneLayout: props.rightPaneLayout,
  rootSplitPaneRef,
  editorSplitPaneRef,
});

// Dropping input/output items
const dropEventHandler = ref<(payload: DragEvent) => void>();
const onDropEventHandlerCreated = (handler: (payload: DragEvent) => void) => {
  dropEventHandler.value = handler;
};

// Menu items and settings pane
const showSettingsPage = ref(false);
const onMenuItemClicked = (args: { event: Event; item: SettingsMenuItem }) => {
  showSettingsPage.value = Boolean(args.item.showSettingsPage);

  // TODO: handle click actions for common items here instead of calling the emit
  emit("menu-item-clicked", args);
};

// Convenient to have this computed property for reactive components
const showControlBarDynamic = computed(() => {
  return props.showControlBar && !collapseAllPanes.value;
});

// We need either filename+language, or provided editor slot
if (props.fileName === null && !useSlots().editor) {
  throw new Error("either fileName or editor slot must be provided");
}
</script>

<template>
  <div class="layout">
    <HeaderBar
      v-if="!collapseAllPanes && title !== null"
      :title="title!"
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
        'slim-mode': collapseAllPanes,
        'left-facing-splitter': !isLeftPaneCollapsed,
        'right-facing-splitter': isLeftPaneCollapsed,
        'collapse-left-pane': collapseLeftPane,
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
        v-show="!collapseLeftPane"
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
                'slim-splitter': !isRightPaneCollapsable,
                'left-facing-splitter': isRightPaneCollapsed,
                'right-facing-splitter': !isRightPaneCollapsed,
              }"
              :dbl-click-splitter="false"
              @splitter-click="
                isRightPaneCollapsable ? collapsePane('right') : undefined
              "
              @resized="resizePane($event[1].size, 'right')"
            >
              <pane
                ref="editorSplitPane"
                data-testid="editorPane"
                :size="usedHorizontalCodeEditorPaneSize"
                min-size="25"
              >
                <div class="editor-and-control-bar">
                  <div
                    class="multi-editor-container"
                    :class="{
                      'has-control-bar': showControlBarDynamic,
                    }"
                  >
                    <template v-if="$slots.editor">
                      <div class="editor-slot-container">
                        <slot name="editor" />
                      </div>
                    </template>
                    <template v-else>
                      <MainEditorPane
                        :file-name="props.fileName!"
                        :language="props.language"
                        :show-control-bar="showControlBarDynamic"
                        :drop-event-handler="dropEventHandler"
                        :to-settings="props.toSettings"
                      />
                    </template>
                    <div class="run-button-panel">
                      <CodeEditorControlBar
                        v-if="showControlBarDynamic"
                        :current-pane-sizes="currentPaneSizes"
                        :show-button-text="showButtonText"
                      >
                        <template #controls>
                          <slot
                            name="code-editor-controls"
                            :show-button-text="showButtonText"
                          />
                        </template>
                      </CodeEditorControlBar>
                    </div>
                  </div>
                </div>
              </pane>
              <pane
                data-testid="rightPane"
                :size="currentPaneSizes.right"
                class="right-pane"
                :min-size="minRatioOfRightPaneInPercent"
              >
                <slot name="right-pane" />
              </pane>
            </splitpanes>
          </pane>
          <pane data-testid="bottomPane" :size="currentPaneSizes.bottom">
            <ScriptingEditorBottomPane
              :slotted-tabs="additionalBottomPaneTabContent"
            >
              <template
                v-for="tab in additionalBottomPaneTabContent"
                #[tab.value]="{ grabFocus }"
              >
                <slot :name="tab.value" :grab-focus="grabFocus" />
              </template>
              <template #console-status>
                <slot name="console-status" />
              </template>
            </ScriptingEditorBottomPane>
          </pane>
        </splitpanes>
      </pane>
    </splitpanes>
  </div>
</template>

<style lang="postcss" scoped>
@import url("@/components/splitterstyles.pcss");

.layout {
  display: flex;
  flex-direction: column;
  height: calc(100vh);
  width: 100%;
  flex-grow: 0;
  overflow: hidden;
  position: relative;
}

.editor-and-control-bar {
  height: 100%;
  display: flex;
  flex-direction: column;

  & .multi-editor-container {
    display: flex;
    flex-direction: column;
    width: 100%;
    flex-grow: 1;
    min-height: 0;

    & .editor-slot-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow-y: auto;
      flex-grow: 1;
    }

    & .run-button-panel {
      display: flex;
      flex-direction: row;
      justify-content: flex-end;
      align-items: center;
      height: fit-content;
      margin: 0;
      background-color: var(--knime-gray-light-semi);
      background-clip: padding-box;
    }
  }
}

.right-pane {
  background-color: var(--knime-gray-ultra-light);
}

.scrollable-y {
  overflow-y: auto;
}
</style>
