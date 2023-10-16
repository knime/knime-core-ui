<script lang="ts">
import { defineComponent, toRaw, type PropType, ref } from "vue";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import CodeEditor from "./CodeEditor.vue";
import FooterBar from "./FooterBar.vue";
import HeaderBar from "./HeaderBar.vue";
import SettingsPage, { type SettingsMenuItem } from "./SettingsPage.vue";
import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";
import type { editor } from "monaco-editor";
import OutputConsole from "./OutputConsole.vue";
import type { ConsoleHandler } from "./OutputConsole.vue";
import { getScriptingService } from "@/scripting-service";
import InputOutputPane from "./InputOutputPane.vue";
import TabBar from "webapps-common/ui/components/TabBar.vue";
import { onKeyStroke } from "@vueuse/core";

export type PaneSizes = {
  [key in "left" | "right" | "bottom"]: number;
};

const commonMenuItems: MenuItem[] = [
  // TODO: add actual common menu items
];

export default defineComponent({
  name: "ScriptingEditor",
  components: {
    Splitpanes,
    Pane,
    CodeEditor,
    FooterBar,
    HeaderBar,
    CodeEditorControlBar,
    OutputConsole,
    InputOutputPane,
    SettingsPage,
    TabBar,
  },
  props: {
    title: {
      type: String,
      default: null,
    },
    language: {
      type: String,
      default: null,
    },
    fileName: {
      type: String,
      default: null,
    },
    rightPaneLayout: {
      type: String as PropType<"fixed" | "relative">,
      default: "fixed",
    },
    menuItems: {
      type: Array<MenuItem>,
      default: [],
    },
    showControlBar: {
      type: Boolean,
      default: true,
    },
    initialPaneSizes: {
      type: Object as PropType<PaneSizes>,
      default: {
        left: 20,
        right: 25,
        bottom: 30,
      } as PaneSizes,
    },
  },
  emits: ["monaco-created", "menu-item-clicked", "save-settings"],
  data() {
    return {
      currentPaneSizes: {
        left: this.initialPaneSizes.left,
        right: this.initialPaneSizes.right,
        bottom: this.initialPaneSizes.bottom,
      } as PaneSizes,
      previousPaneSizes: {
        left: this.initialPaneSizes.left,
        right: this.initialPaneSizes.right,
        bottom: this.initialPaneSizes.bottom,
      } as PaneSizes,
      commonMenuItems,
      editorModel: null as editor.ITextModel | null,
      showSettingsPage: false,
      bottomPaneOptions: [{ value: "console", label: "Console" }],
      bottomPaneActiveTab: ref("console"),
      dropEventHandler: null as Function | null,
    };
  },
  computed: {
    usedMainPaneSize() {
      return 100 - this.currentPaneSizes.left;
    },
    usedHorizontalCodeEditorPaneSize() {
      return 100 - this.currentPaneSizes.right;
    },
    usedVerticalCodeEditorPaneSize() {
      return 100 - this.currentPaneSizes.bottom;
    },
    isLeftPaneCollapsed() {
      return this.currentPaneSizes.left === 0;
    },
    isRightPaneCollapsed() {
      return this.currentPaneSizes.right === 0;
    },
    isBottomPaneCollapsed() {
      return this.currentPaneSizes.bottom === 0;
    },
  },
  methods: {
    collapsePane(pane: keyof PaneSizes) {
      let newSize =
        this.currentPaneSizes[pane] === 0 ? this.previousPaneSizes[pane] : 0;
      if (pane === "left") {
        this.updateRightPane(newSize);
      }
      this.resizePane(newSize, pane);
    },
    resizePane(
      size: number,
      pane: keyof PaneSizes,
      updatePreviousPaneSize: boolean = true,
    ) {
      this.currentPaneSizes[pane] = size;

      if (updatePreviousPaneSize) {
        this.updatePreviousPaneSize(pane);
      }
    },
    updatePreviousPaneSize(pane: keyof PaneSizes) {
      if (this.currentPaneSizes[pane] <= 0) {
        return;
      }
      this.previousPaneSizes[pane] = this.currentPaneSizes[pane];
    },
    updateRightPane(size: number) {
      if (
        this.rightPaneLayout !== "fixed" ||
        this.currentPaneSizes.right <= 0
      ) {
        return;
      }
      // keep right pane at same size when left pane is resized
      const newMainPaneSize = 100 - size;
      const absoluteRightPaneSize =
        (100 - this.currentPaneSizes.left) *
        (this.previousPaneSizes.right / 100);
      const newRightPaneSize = (absoluteRightPaneSize / newMainPaneSize) * 100;

      this.resizePane(newRightPaneSize, "right", false);
    },
    onMonacoCreated({
      editor,
      editorModel,
    }: {
      editor: editor.IStandaloneCodeEditor;
      editorModel: editor.ITextModel;
    }) {
      this.editorModel = editorModel;
      getScriptingService().initEditorService(editor, editorModel);
      this.$emit("monaco-created", { editor, editorModel });

      // register Key
      // undo changes from outside the editor
      onKeyStroke("z", (e) => {
        const key = navigator.userAgent.toLowerCase().includes("mac")
          ? e.metaKey
          : e.ctrlKey;
        if (key) {
          editor.trigger("window", "undo", {});
        }
      });
    },

    saveSettings() {
      const editorModel = toRaw(this.editorModel);
      const settings = { script: editorModel?.getValue() ?? "" };
      this.$emit("save-settings", settings);
    },
    closeDialog() {
      getScriptingService().closeDialog();
    },
    onMenuItemClicked(args: { event: Event; item: SettingsMenuItem }) {
      this.showSettingsPage = Boolean(args.item.showSettingsPage);

      if (commonMenuItems.includes(toRaw(args.item))) {
        // TODO: handle click actions for common items here
      } else {
        this.$emit("menu-item-clicked", args);
      }
    },
    onDropEventHandlerCreated(dropEventHandler: Function) {
      this.dropEventHandler = dropEventHandler;
    },
    onConsoleCreated(handler: ConsoleHandler) {
      getScriptingService().registerConsoleEventHandler(handler);
    },
  },
});
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
      ref="mainSplitpane"
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
      <pane ref="leftPane" :size="currentPaneSizes.left">
        <InputOutputPane
          @drop-event-handler-created="onDropEventHandlerCreated"
        />
      </pane>
      <pane ref="mainPane" :size="usedMainPaneSize" min-size="40">
        <splitpanes
          ref="horizontalSplitpane"
          horizontal
          class="common-splitter"
          :dbl-click-splitter="false"
          :class="{
            'down-facing-splitter': !isBottomPaneCollapsed,
            'up-facing-splitter': isBottomPaneCollapsed,
          }"
          @splitter-click="collapsePane('bottom')"
          @resize="resizePane($event[1].size, 'bottom')"
        >
          <pane
            ref="topPane"
            :size="usedVerticalCodeEditorPaneSize"
            min-size="40"
          >
            <splitpanes
              ref="verticalSplitpane"
              class="common-splitter unset-transition"
              :class="{
                'left-facing-splitter': isRightPaneCollapsed,
                'right-facing-splitter': !isRightPaneCollapsed,
              }"
              :dbl-click-splitter="false"
              @splitter-click="collapsePane('right')"
              @resized="resizePane($event[1].size, 'right')"
            >
              <pane
                ref="editorPane"
                :size="usedHorizontalCodeEditorPaneSize"
                min-size="25"
              >
                <CodeEditor
                  :language="language"
                  :file-name="fileName"
                  class="code-editor"
                  @drop="
                    (event: DragEvent) => {
                      dropEventHandler !== null
                        ? dropEventHandler(event)
                        : null;
                    }
                  "
                  @monaco-created="onMonacoCreated"
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
              <pane ref="rightPane" :size="currentPaneSizes.right">
                <slot name="right-pane" />
              </pane>
            </splitpanes>
          </pane>
          <pane ref="bottomPane" :size="currentPaneSizes.bottom">
            <div class="tab-bar-container">
              <div class="tab-bar-wrapper">
                <TabBar
                  v-model="bottomPaneActiveTab"
                  class="bottom-tab-bar"
                  :possible-values="bottomPaneOptions"
                />
              </div>
              <div class="console-container">
                <OutputConsole
                  v-show="bottomPaneActiveTab === 'console'"
                  @console-created="onConsoleCreated"
                />
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

  & .tab-bar-wrapper {
    margin-right: 10px;

    & .bottom-tab-bar {
      & :deep(.tab-bar) {
        padding-bottom: 0;
        margin-bottom: 0;
      }

      & :deep(.carousel::after) {
        bottom: 5px;
      }
    }
  }

  & .console-container {
    flex: 1;
    position: relative;
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
}

.settings-page {
  height: calc(100vh - var(--controls-height));
}

/* stylelint-enable selector-class-pattern */
</style>
