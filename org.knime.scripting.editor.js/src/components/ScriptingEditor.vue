<script lang="ts">
import { defineComponent, toRaw, type PropType } from "vue";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import CodeEditor from "./CodeEditor.vue";
import FooterBar from "./FooterBar.vue";
import HeaderBar from "./HeaderBar.vue";
import CodeEditorControlBar from "./CodeEditorControlBar.vue";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";
import type { editor } from "monaco-editor";
import OutputConsole from "./OutputConsole.vue";
import type { ConsoleHandler } from "./OutputConsole.vue";

import { getScriptingService } from "@/scripting-service";
import InputOutputPane from "./InputOutputPane.vue";

type PaneSizes = {
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
  },
  emits: ["monaco-created", "menu-item-clicked"],
  data() {
    return {
      currentPaneSizes: {
        left: 20,
        right: 25,
        bottom: 30,
      } as PaneSizes,
      initialPaneSizes: {
        left: 20,
        right: 25,
        bottom: 30,
      } as PaneSizes,
      previousPaneSizes: {
        left: 20,
        right: 25,
        bottom: 30,
      } as PaneSizes,
      commonMenuItems,
      editorModel: null as editor.ITextModel | null,
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
    },
    saveSettings() {
      const editorModel = toRaw(this.editorModel);
      if (editorModel) {
        getScriptingService().saveSettings({ script: editorModel.getValue() });
        this.closeDialog();
      }
    },
    closeDialog() {
      getScriptingService().closeDialog();
    },
    onMenuItemClicked(args: { event: Event; item: any }) {
      if (commonMenuItems.includes(toRaw(args.item))) {
        // TODO: handle click actions for common items here
      } else {
        this.$emit("menu-item-clicked", args);
      }
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
    <splitpanes
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
      @resize="updateRightPane($event[0].size)"
      @resized="
        resizePane($event[0].size, 'left');
        updatePreviousPaneSize('right');
      "
    >
      <pane ref="leftPane" :size="currentPaneSizes.left">
        <InputOutputPane />
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
          @resized="resizePane($event[1].size, 'bottom')"
        >
          <pane
            ref="topPane"
            :size="usedVerticalCodeEditorPaneSize"
            min-size="20"
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
                  @monaco-created="onMonacoCreated"
                />
                <CodeEditorControlBar v-if="showControlBar">
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
            <OutputConsole @console-created="onConsoleCreated" />
          </pane>
        </splitpanes>
      </pane>
    </splitpanes>
    <FooterBar
      @scripting-editor-okayed="saveSettings"
      @scripting-editor-cancelled="closeDialog"
    />
  </div>
</template>

<style lang="postcss" scoped>
.layout {
  --controls-height: 49px;
  --description-button-size: 15px;

  display: flex;
  flex-direction: column;
  height: calc(100vh);
  width: 100%;
  border-left: 1px solid var(--knime-silver-sand);
  border-right: 1px solid var(--knime-silver-sand);
  flex-grow: 0;
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
}

/* stylelint-enable selector-class-pattern */
</style>
