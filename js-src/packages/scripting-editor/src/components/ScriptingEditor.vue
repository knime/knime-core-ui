<script setup lang="ts">
import { computed, ref, useSlots, useTemplateRef, watchEffect } from "vue";
import { useElementBounding } from "@vueuse/core";

import { type MenuItem, SplitPanel } from "@knime/components";
import "@knime/kds-styles/kds-variables.css";

import { displayMode } from "../display-mode";
import { getInitialData } from "../init";

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
import {
  MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
  type PaneSizes,
} from "./utils/paneSizes";

// Props
interface Props {
  title?: string | null;
  language: string;
  fileName?: string | null;
  menuItems?: MenuItem[];
  showControlBar?: boolean;
  /**
   * In pixels.
   */
  initialPaneSizes?: PaneSizes;
  /**
   * Called on apply if the main editor is being used. This function must be implemented
   * to join the settings from the main editor and other settings like from the
   * NodeParametersPanel into a single settings object as expected by the backend.
   *
   * A simple implementation for the `DefaultScriptingNodeSettingsService` looks like this:
   * ```ts
   * const toSettings = (commonSettings) =>
   *   joinSettings(
   *     commonSettings,
   *     nodeParametersPanel.value?.getDataAndFlowVariableSettings(),
   *   );
   * ```
   *
   * @param settings the current settings from the main editor
   * @returns the joined settings as expected by the backend
   */
  toSettings?: (settings: { script: string }) => unknown;
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
  menuItems: () => [],
  showControlBar: true,
  initialPaneSizes: () => ({ left: 260, right: 260, bottom: 300 }),
  additionalBottomPaneTabContent: () => [] as SlottedTab[],
  toSettings: (settings: { script: string }) => settings,
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

const emit = defineEmits(["menu-item-clicked"]);

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

// We need either filename+language, or provided editor slot
if (props.fileName === null && !useSlots().editor) {
  throw new Error("either fileName or editor slot must be provided");
}

const getInputOutputItemsFromInitialData = () => {
  const initialData = getInitialData();
  return [
    ...initialData.inputObjects,
    initialData.flowVariables,
    ...(initialData.outputObjects ?? []),
  ];
};
const defaultInputOutputItems = slots["left-pane"]
  ? []
  : getInputOutputItemsFromInitialData();

// #region ================= PANE SIZES ====================

const isSmallEmbeddedMode = computed(() => displayMode.value === "small");

// We need to track the expanded state of each pane (otherwise they won't be collapsible)
const rightPaneExpanded = ref(true);
const leftPaneExpanded = ref(true);
const bottomPaneExpanded = ref(true);

// Keep track of the pane sizes so we can pass them to components that need to know them
const rightPaneSize = ref<number>(props.initialPaneSizes.right);
const leftPaneSize = ref<number>(props.initialPaneSizes.left);
const bottomPaneSize = ref<number>(props.initialPaneSizes.bottom);
const currentPaneSizes = computed(() => ({
  left: leftPaneSize.value,
  right: rightPaneSize.value,
  bottom: bottomPaneSize.value,
}));

// Determine if the editor is wide enough to show the button text in the control bar
const editorSplitPane = ref();
const editorSplitPaneRef = useElementBounding(editorSplitPane);
const showButtonText = computed(
  () =>
    displayMode.value !== "small" &&
    editorSplitPaneRef.width.value >= MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
);

// The control bar is show if set in props and not in small mode
const showControlBarDynamic = computed(
  () => props.showControlBar && !isSmallEmbeddedMode.value,
);

// Watch if the bottom pane has tabs to decide whether to show or hide it
const bottomPane = useTemplateRef("bottomPane");
const hasBottomPaneTabs = ref(false);
watchEffect(() => {
  hasBottomPaneTabs.value = bottomPane.value?.hasTabs ?? false;
});

// #endregion
</script>

<template>
  <div class="layout">
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

    <HeaderBar
      v-if="
        !isSmallEmbeddedMode &&
        !showSettingsPage &&
        (menuItems.length > 0 || title != null)
      "
      :title="title"
      :menu-items="menuItems"
      @menu-item-click="onMenuItemClicked"
    />

    <SplitPanel
      v-show="!showSettingsPage"
      v-model:expanded="leftPaneExpanded"
      v-model:secondary-size="leftPaneSize"
      :hide-secondary-pane="isSmallEmbeddedMode"
      direction="left"
      use-pixel
      keep-element-on-close
      :secondary-snap-size="180"
      class="vertical-splitpanel allow-splitter-overflow-right-pane"
    >
      <template #secondary>
        <slot name="left-pane">
          <InputOutputPane
            :input-output-items="defaultInputOutputItems"
            @drop-event-handler-created="onDropEventHandlerCreated"
          />
        </slot>
      </template>

      <SplitPanel
        v-model:expanded="bottomPaneExpanded"
        v-model:secondary-size="bottomPaneSize"
        :hide-secondary-pane="isSmallEmbeddedMode || !hasBottomPaneTabs"
        direction="down"
        use-pixel
        keep-element-on-close
        :secondary-snap-size="200"
        class="horizontal-splitpanel allow-splitter-overflow-top-pane"
      >
        <SplitPanel
          v-model:expanded="rightPaneExpanded"
          v-model:secondary-size="rightPaneSize"
          :hide-secondary-pane="isSmallEmbeddedMode || !$slots['right-pane']"
          direction="right"
          :secondary-snap-size="220"
          use-pixel
          keep-element-on-close
          splitter-id="verticalSplitpane"
          data-testid="verticalSplitpane"
          class="vertical-splitpanel allow-splitter-overflow-left-pane"
        >
          <div
            ref="editorSplitPane"
            data-testid="editorPane"
            class="editor-pane"
          >
            <div class="editor-and-control-bar">
              <div
                class="multi-editor-container"
                :class="{ 'has-control-bar': showControlBarDynamic }"
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
                    :show-button-text
                  >
                    <template #controls>
                      <slot name="code-editor-controls" :show-button-text />
                    </template>
                  </CodeEditorControlBar>
                </div>
              </div>
            </div>
          </div>
          <template #secondary>
            <div data-testid="rightPane" class="right-pane">
              <slot name="right-pane" />
            </div>
          </template>
        </SplitPanel>
        <template #secondary>
          <ScriptingEditorBottomPane
            ref="bottomPane"
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
        </template>
      </SplitPanel>
    </SplitPanel>
  </div>
</template>

<style lang="postcss" scoped>
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

.editor-pane {
  height: 100%;
  width: 100%;
}

.scrollable-y {
  overflow-y: auto;
}

/* NOTE: The AI popup needs to overflow the panes but positioned relative.
 * Therefore, this hack is needed for now. */
.allow-splitter-overflow-right-pane {
  overflow: visible;
  min-height: 0;
  min-width: 0;

  &:deep(> .splitter-pane.right-pane) {
    overflow: visible;
    min-width: 0;
    min-height: 0;
  }
}

.allow-splitter-overflow-left-pane {
  overflow: visible;
  min-width: 0;
  min-height: 0;

  &:deep(> .splitter-pane.left-pane) {
    overflow: visible;
    min-width: 0;
    min-height: 0;
  }
}

.allow-splitter-overflow-top-pane {
  overflow: visible;
  min-height: 0;
  min-width: 0;

  &:deep(> .splitter-pane.top-pane) {
    overflow: visible;
    min-height: 0;
    min-width: 0;
  }
}

.vertical-splitpanel,
.horizontal-splitpanel {
  height: 100%;
  width: 100%;
}

.right-pane {
  background-color: var(--knime-gray-ultra-light);
  height: 100%;
}
</style>
