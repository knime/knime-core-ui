<script setup lang="ts">
import { computed, ref, useSlots } from "vue";
import { computedAsync, useElementBounding } from "@vueuse/core";
import { Pane, Splitpanes } from "splitpanes";

import "splitpanes/dist/splitpanes.css";
import type { MenuItem } from "@knime/components";

import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import { type PaneSizes } from "@/components/utils/paneSizes";
import { useResizeLogic } from "@/components/utils/resizeLogic";
import { getInitialDataService } from "@/initial-data-service";
import { type GenericNodeSettings } from "@/settings-service";

import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import HeaderBar from "./HeaderBar.vue";
import InputOutputPane from "./InputOutputPane.vue";
import MainEditorPane from "./MainEditorPane.vue";
import ScriptingEditorBottomPane, {
  type BottomPaneTabControlsSlotName,
  type BottomPaneTabSlotName,
  type SlottedTab,
} from "./ScriptingEditorBottomPane.vue";
import type { SettingsMenuItem } from "./SettingsPage.vue";
import SettingsPage from "./SettingsPage.vue";

const commonMenuItems: MenuItem[] = [
  // TODO: add actual common menu items
];

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
  additionalBottomPaneTabContent?: SlottedTab[];
  /*
   * When using the single editor pane, this prop can be used to determine
   * whether the editor is used for a model or a view.
   * This will influence how the dirty state is handled.
   * default: "model"
   */
  modelOrView?: "model" | "view";
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
  additionalBottomPaneTabContent: () => [] as SlottedTab[],
  toSettings: (settings: GenericNodeSettings) => settings,
  modelOrView: "model",
});

/* eslint-disable @typescript-eslint/no-explicit-any */
// The return type of the slots is any as per the Vue 3 documentation.
const slots = defineSlots<{
  "left-pane": () => any;
  editor: () => any;
  "settings-title": () => any;
  "settings-content": () => any;
  "right-pane": () => any;
  "code-editor-controls": (props: { showButtonText: boolean }) => any;
  "bottom-pane-status-label": () => any;
  [key: BottomPaneTabSlotName]: (props: { grabFocus: () => void }) => any;
  [key: BottomPaneTabControlsSlotName]: () => any;
}>();
/* eslint-enable @typescript-eslint/no-explicit-any */

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
  doResizePane,
  doUpdatePreviousPaneSize,
  doUpdateRightPane,
  doToggleCollapsePane,
  currentPaneSizes,
  shouldCollapseAllPanes,
  shouldCollapseLeftPane,
  shouldShowButtonText,
  isBottomPaneCollapsed,
  isLeftPaneCollapsed,
  isRightPaneCollapsed,
  minRatioOfRightPaneInPercent,
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
  return props.showControlBar && !shouldCollapseAllPanes.value;
});

// We need either filename+language, or provided editor slot
if (props.fileName === null && !useSlots().editor) {
  throw new Error("either fileName or editor slot must be provided");
}

const defaultInputOutputItems = computedAsync<InputOutputModel[]>(async () => {
  if (slots["left-pane"]) {
    return [];
  } else {
    const initialData = await getInitialDataService().getInitialData();
    const initialItems = [
      ...initialData.inputObjects,
      initialData.flowVariables,
    ];

    if (initialData.outputObjects) {
      initialItems.push(...initialData.outputObjects);
    }

    return initialItems;
  }
}, []);
</script>

<template>
  <div class="layout">
    <HeaderBar
      v-if="!shouldCollapseAllPanes && title !== null"
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
        'slim-mode': shouldCollapseAllPanes,
        'left-facing-splitter': !isLeftPaneCollapsed,
        'right-facing-splitter': isLeftPaneCollapsed,
        'collapse-left-pane': shouldCollapseLeftPane,
      }"
      @splitter-click="
        doToggleCollapsePane('left');
        doUpdatePreviousPaneSize('right');
      "
      @resize="
        doUpdateRightPane($event[0].size);
        doResizePane($event[0].size, 'left', false);
        doUpdatePreviousPaneSize('right');
      "
      @resized="doUpdatePreviousPaneSize('left')"
    >
      <pane
        v-show="!shouldCollapseLeftPane"
        data-testid="leftPane"
        :size="currentPaneSizes.left"
        class="scrollable-y"
      >
        <slot name="left-pane">
          <InputOutputPane
            :input-output-items="defaultInputOutputItems"
            @drop-event-handler-created="onDropEventHandlerCreated"
          />
        </slot>
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
          @splitter-click="doToggleCollapsePane('bottom')"
          @resize="doResizePane($event[1].size, 'bottom')"
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
                isRightPaneCollapsable
                  ? doToggleCollapsePane('right')
                  : undefined
              "
              @resized="doResizePane($event[1].size, 'right')"
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
                        :model-or-view="props.modelOrView"
                      />
                    </template>
                    <div class="run-button-panel">
                      <CodeEditorControlBar
                        v-if="showControlBarDynamic"
                        :current-pane-sizes="currentPaneSizes"
                        :show-button-text="shouldShowButtonText"
                      >
                        <template #controls>
                          <slot
                            name="code-editor-controls"
                            :show-button-text="shouldShowButtonText"
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
          <pane
            data-testid="bottomPane"
            class="bottom-pane"
            :size="currentPaneSizes.bottom"
          >
            <ScriptingEditorBottomPane
              :slotted-tabs="additionalBottomPaneTabContent"
            >
              <template
                v-for="tab in additionalBottomPaneTabContent"
                #[tab.slotName]="{ grabFocus }"
              >
                <slot :name="tab.slotName" :grab-focus="grabFocus" />
              </template>
              <template
                v-for="tab in additionalBottomPaneTabContent"
                #[tab.associatedControlsSlotName!]
              >
                <slot
                  v-if="tab.associatedControlsSlotName"
                  :name="tab.associatedControlsSlotName"
                />
              </template>
              <template #status-label>
                <slot name="bottom-pane-status-label" />
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
