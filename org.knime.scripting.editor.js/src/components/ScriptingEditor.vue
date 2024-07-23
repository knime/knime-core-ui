<script setup lang="ts">
import { useElementBounding } from "@vueuse/core";

import { computed, onMounted, type Ref, ref, useSlots } from "vue";
import { Pane, Splitpanes } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import type { MenuItem } from "@knime/components";
import {
  getScriptingService,
  initConsoleEventHandler,
  type NodeSettings,
  type PortConfigs,
} from "@/scripting-service";
import CompactTabBar from "./CompactTabBar.vue";
import HeaderBar from "./HeaderBar.vue";
import InputOutputPane from "./InputOutputPane.vue";
import type { ConsoleHandler } from "./OutputConsole.vue";
import OutputConsole from "./OutputConsole.vue";
import type { SettingsMenuItem } from "./SettingsPage.vue";
import SettingsPage from "./SettingsPage.vue";
import { setConsoleHandler } from "@/consoleHandler";
import MainEditorPane from "./MainEditorPane.vue";
import { type PaneSizes } from "@/components/utils/paneSizes";
import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import useShouldFocusBePainted from "@/components/utils/shouldFocusBePainted";
import InputPortTables from "@/components/InputPortTables.vue";
import { useMainCodeEditorStore } from "@/editor";
import OutputTablePreview from "@/components/OutputTablePreview.vue";
import { useResizeLogic } from "@/components/utils/resizeLogic";

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
  toSettings?: (settings: NodeSettings) => NodeSettings;
  showOutputTable?: boolean;
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
  toSettings: (settings: NodeSettings) => settings,
  showOutputTable: false,
});

const isRightPaneCollapsable = computed(
  () => props.rightPaneMinimumWidthInPixel === 0,
);
const emit = defineEmits(["menu-item-clicked", "input-output-item-insertion"]);

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

const portConfigs: PortConfigs = {
  inputPorts: [],
};

const initalBottomPaneOptions = [{ value: "console", label: "Console" }];

if (props.showOutputTable) {
  initalBottomPaneOptions.push({ value: "outputTable", label: "Output table" });
}

const bottomPaneOptions: Ref<{ value: string; label: string }[]> = ref(
  initalBottomPaneOptions,
);

const bottomPaneActiveTab = ref<string>(bottomPaneOptions.value[0].value);

const makeNodePortId = (nodeId: string, portIdx: number) =>
  `${nodeId}-${portIdx}`;

onMounted(async () => {
  portConfigs.inputPorts = (
    await getScriptingService().getPortConfigs()
  ).inputPorts.filter((port) => port.nodeId !== null);

  if (
    portConfigs.inputPorts.length !== 0 &&
    (await getScriptingService().isCallKnimeUiApiAvailable(
      portConfigs.inputPorts[0],
    ))
  ) {
    const inputPorts = portConfigs.inputPorts.reverse().map((port, index) => ({
      value: makeNodePortId(port.nodeId!, port.portIdx),
      label: `${index}: ${port.portName}`,
    }));
    bottomPaneOptions.value = [...inputPorts, ...bottomPaneOptions.value];
  }
});

const onConsoleCreated = (handler: ConsoleHandler) => {
  setConsoleHandler(handler);
  initConsoleEventHandler();
};

const slots = useSlots();

const onInputOutputItemInsertion = (
  codeAlias: string,
  requiredImport: string | undefined,
) => {
  if (slots.editor) {
    // If we got passed something via slot, we have to emit the event since we
    // can't execute the edit ourselves.
    emit("input-output-item-insertion", codeAlias, requiredImport);
  } else if (
    // But if we're responsible for the editor, we can just insert directly.
    requiredImport &&
    !useMainCodeEditorStore().value?.text.value.includes(requiredImport)
  ) {
    useMainCodeEditorStore().value?.insertColumnReference(
      codeAlias,
      requiredImport,
    );
  } else {
    useMainCodeEditorStore().value?.insertColumnReference(codeAlias);
  }
};

// Convenient to have this computed property for reactive components
const showControlBarDynamic = computed(() => {
  return props.showControlBar && !collapseAllPanes.value;
});

// We need either filename+language, or provided editor slot
if (props.fileName === null && !useSlots().editor) {
  throw new Error("either fileName or editor slot must be provided");
}

const paintFocus = useShouldFocusBePainted();
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
          @input-output-item-insertion="onInputOutputItemInsertion"
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
                        :language="language"
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
            <div class="tab-bar-container">
              <CompactTabBar
                v-model="bottomPaneActiveTab"
                class="scripting-editor-tab-bar"
                :possible-values="bottomPaneOptions"
                :class="{ 'focus-painted': paintFocus }"
              />
              <div class="console-container">
                <OutputConsole
                  v-show="bottomPaneActiveTab === 'console'"
                  class="console"
                  @console-created="onConsoleCreated"
                >
                  <template #console-status>
                    <slot name="console-status" />
                  </template>
                </OutputConsole>
                <div
                  v-for="port in portConfigs?.inputPorts"
                  :key="port.portIdx"
                  class="port-tables"
                  :class="{
                    collapsed:
                      bottomPaneActiveTab !==
                        makeNodePortId(port.nodeId!, port.portIdx) &&
                      port.nodeId,
                  }"
                >
                  <InputPortTables
                    v-if="
                      bottomPaneActiveTab ===
                        makeNodePortId(port.nodeId!, port.portIdx) &&
                      port.nodeId
                    "
                    :input-node-id="port.nodeId"
                    :port-idx="port.portIdx"
                    :port-view-configs="port.portViewConfigs"
                  />
                </div>
                <template v-if="showOutputTable">
                  <div
                    v-show="bottomPaneActiveTab === 'outputTable'"
                    class="port-tables"
                    :class="{
                      collapsed: bottomPaneActiveTab !== 'outputTable',
                    }"
                  >
                    <OutputTablePreview
                      @output-table-updated="
                        () => (bottomPaneActiveTab = 'outputTable')
                      "
                    />
                  </div>
                </template>
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
  --description-button-size: 15px;

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
}

.tab-bar-container {
  display: flex;
  height: 100%;
  flex-direction: column;
  position: relative;

  & .console-container {
    flex: 1;
    height: 100%;
    min-height: 0;

    & .console {
      padding: 0 var(--space-8);
    }
  }
}

.port-tables {
  height: 100%;
  display: flex;
  flex-grow: 1;
}

.port-tables.collapsed {
  display: none;
}

.editor-slot-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  flex-grow: 1;
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

.collapse-left-pane {
  & :deep(> .splitpanes__splitter) {
    display: none;
    pointer-events: none;
  }
}

.common-splitter.slim-splitter :deep(.splitpanes__splitter) {
  position: relative;
  min-width: 2px;
  border-width: 0 0 0 1px;

  &::before {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 11px;
    height: 100%;
    background: transparent;
    cursor: ew-resize;
    z-index: 1;
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
    background-image: url("@knime/styles/img/icons/arrow-prev.svg");
  }
}

.right-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("@knime/styles/img/icons/arrow-next.svg");
  }
}

.down-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("@knime/styles/img/icons/arrow-dropdown.svg");
  }
}

.up-facing-splitter {
  & :deep(> .splitpanes__splitter) {
    background-image: url("@knime/styles/img/icons/arrow-dropdown.svg");
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

/* We need to reduce the size of this pane slightly iff there's a control bar */
.multi-editor-container.has-control-bar {
  height: auto;
}

.multi-editor-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  flex-grow: 1;
  min-height: 0;
}

.run-button-panel {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  height: fit-content;
  margin: 0;
  background-color: var(--knime-gray-light-semi);
  background-clip: padding-box;
}
</style>
