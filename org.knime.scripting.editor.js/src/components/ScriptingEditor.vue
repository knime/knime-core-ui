<script lang="ts">
import { defineComponent, type PropType } from "vue";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import CodeEditor from "./CodeEditor.vue";
import { type PaneSizes, RightPaneLayout } from "../types/scriptingEditor";

export default defineComponent({
  name: "ScriptingEditor",
  components: {
    Splitpanes,
    Pane,
    CodeEditor,
  },
  props: {
    language: {
      type: String,
      default: null,
    },
    rightPaneLayout: {
      type: String as PropType<RightPaneLayout>,
      default: RightPaneLayout.FIXED,
    },
  },
  emits: ["monaco-created"],
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
      this.resizePane(newSize, pane);
    },
    resizePane(size: number, pane: keyof PaneSizes) {
      if (
        pane === "left" &&
        this.currentPaneSizes.right > 0 &&
        this.rightPaneLayout === RightPaneLayout.FIXED
      ) {
        this.updateRightPane(size);
      }

      this.currentPaneSizes[pane] = size;

      if (this.currentPaneSizes[pane] > 0) {
        this.previousPaneSizes[pane] = this.currentPaneSizes[pane];
      }
    },
    updateRightPane(size: number) {
      // keep right pane at same size when left pane is resized
      const currentMainPane = 100 - this.currentPaneSizes.left;
      const newMainPane = 100 - size;
      const currentAbsoluteRightPaneSize =
        (this.currentPaneSizes.right / 100) * currentMainPane;
      const newRightPaneSize =
        (currentAbsoluteRightPaneSize / newMainPane) * 100;
      this.resizePane(newRightPaneSize, "right");
    },
  },
});
</script>

<template>
  <div class="layout">
    <splitpanes
      class="common-splitter unset-transition"
      :dbl-click-splitter="false"
      :class="{
        'left-facing-splitter': !isLeftPaneCollapsed,
        'right-facing-splitter': isLeftPaneCollapsed,
      }"
      @splitter-click="collapsePane('left')"
      @resize="resizePane($event[0].size, 'left')"
    >
      <pane :size="currentPaneSizes.left" />
      <pane :size="usedMainPaneSize" min-size="40">
        <splitpanes
          horizontal
          class="common-splitter down-facing-splitter"
          :dbl-click-splitter="false"
          @splitter-click="collapsePane('bottom')"
          @resize="resizePane($event[1].size, 'bottom')"
        >
          <pane :size="usedVerticalCodeEditorPaneSize" min-size="20">
            <splitpanes
              class="common-splitter unset-transition"
              :class="{
                'left-facing-splitter': isRightPaneCollapsed,
                'right-facing-splitter': !isRightPaneCollapsed,
              }"
              :dbl-click-splitter="false"
              @splitter-click="collapsePane('right')"
              @resize="resizePane($event[1].size, 'right')"
            >
              <pane :size="usedHorizontalCodeEditorPaneSize" min-size="25">
                <CodeEditor initial-script="foo" language="python" />
              </pane>
              <pane :size="currentPaneSizes.right" />
            </splitpanes>
          </pane>
          <pane :size="currentPaneSizes.bottom" />
        </splitpanes>
      </pane>
    </splitpanes>
  </div>
</template>

<style lang="postcss" scoped>
.layout {
  --controls-height: 49px;
  --description-button-size: 15px;

  display: flex;
  flex-direction: column;
  height: calc(100vh - 15px);
  width: 100%;
  border-left: 1px solid var(--knime-silver-sand);
}

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

.splitpanes__pane {
  transition: unset;
}

/* stylelint-enable selector-class-pattern */
</style>
