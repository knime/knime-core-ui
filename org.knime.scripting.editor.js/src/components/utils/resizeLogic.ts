import { computed, reactive } from "vue";
import type { UseElementBoundingReturn } from "@vueuse/core";

import {
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
  type PaneSizes,
} from "@/components/utils/paneSizes";
import { displayMode } from "@/display-mode";

export const useResizeLogic = ({
  initialPaneSizes,
  rightPaneMinimumWidthInPixel,
  rightPaneLayout,
  rootSplitPaneRef,
  editorSplitPaneRef,
}: {
  initialPaneSizes: PaneSizes;
  rightPaneMinimumWidthInPixel: number;
  rightPaneLayout: "fixed" | "relative";
  rootSplitPaneRef: UseElementBoundingReturn;
  editorSplitPaneRef: UseElementBoundingReturn;
}) => {
  const largeModePaneSizes = reactive<PaneSizes>({
    left: initialPaneSizes.left,
    right: initialPaneSizes.right,
    bottom: initialPaneSizes.bottom,
  });
  const previousLargeModePaneSizes = reactive<PaneSizes>({
    left: initialPaneSizes.left,
    right: initialPaneSizes.right,
    bottom: initialPaneSizes.bottom,
  });

  const shouldCollapseAllPanes = computed(
    () =>
      displayMode.value === "small" ||
      rootSplitPaneRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_PANES,
  );
  const shouldCollapseLeftPane = computed(
    () =>
      displayMode.value === "small" ||
      rootSplitPaneRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  );
  const shouldShowButtonText = computed(
    () =>
      displayMode.value === "small" ||
      editorSplitPaneRef.width.value > MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
  );
  const minRatioOfRightPaneInPercent = computed(
    () =>
      (rightPaneMinimumWidthInPixel /
        ((rootSplitPaneRef.width.value * (100 - largeModePaneSizes.left)) /
          100)) *
      100,
  );

  const currentPaneSizes = computed(() => {
    if (shouldCollapseAllPanes.value) {
      return {
        left: 0,
        right: 0,
        bottom: 0,
      };
    } else if (shouldCollapseLeftPane.value) {
      return {
        left: 0,
        right: largeModePaneSizes.right,
        bottom: largeModePaneSizes.bottom,
      };
    } else {
      return {
        ...largeModePaneSizes,
        right: Math.max(
          largeModePaneSizes.right,
          minRatioOfRightPaneInPercent.value,
        ),
      };
    }
  });

  const usedMainPaneSize = computed(() => 100 - currentPaneSizes.value.left);
  const usedHorizontalCodeEditorPaneSize = computed(
    () => 100 - currentPaneSizes.value.right,
  );
  const usedVerticalCodeEditorPaneSize = computed(
    () => 100 - currentPaneSizes.value.bottom,
  );
  const isLeftPaneCollapsed = computed(() => currentPaneSizes.value.left === 0);
  const isRightPaneCollapsed = computed(
    () => currentPaneSizes.value.right === 0,
  );
  const isBottomPaneCollapsed = computed(
    () => currentPaneSizes.value.bottom === 0,
  );
  const doUpdatePreviousPaneSize = (pane: keyof PaneSizes) => {
    if (currentPaneSizes.value[pane] <= 0) {
      return;
    }
    previousLargeModePaneSizes[pane] = largeModePaneSizes[pane];
  };
  const doResizePane = (
    size: number,
    pane: keyof PaneSizes,
    shouldUpdatePreviousPaneSize: boolean = true,
  ) => {
    largeModePaneSizes[pane] = size;

    if (shouldUpdatePreviousPaneSize) {
      doUpdatePreviousPaneSize(pane);
    }
  };
  const doUpdateRightPane = (size: number) => {
    if (rightPaneLayout !== "fixed" || currentPaneSizes.value.right <= 0) {
      return;
    }
    // keep right pane at same size when left pane is resized
    const newMainPaneSize = 100 - size;

    const absoluteRightPaneSize =
      (100 - currentPaneSizes.value.left) *
      (previousLargeModePaneSizes.right / 100);
    const newRightPaneSize = Math.max(
      minRatioOfRightPaneInPercent.value,
      (absoluteRightPaneSize / newMainPaneSize) * 100,
    );

    doResizePane(newRightPaneSize, "right", false);
  };
  const doToggleCollapsePane = (pane: keyof PaneSizes) => {
    const newSize =
      currentPaneSizes.value[pane] === 0 ? previousLargeModePaneSizes[pane] : 0;
    if (pane === "left") {
      doUpdateRightPane(newSize);
    }
    doResizePane(newSize, pane);
  };

  return {
    shouldCollapseAllPanes,
    shouldCollapseLeftPane,
    shouldShowButtonText,
    minRatioOfRightPaneInPercent,
    currentPaneSizes,
    usedMainPaneSize,
    usedHorizontalCodeEditorPaneSize,
    usedVerticalCodeEditorPaneSize,
    isLeftPaneCollapsed,
    isRightPaneCollapsed,
    isBottomPaneCollapsed,
    doUpdatePreviousPaneSize,
    doResizePane,
    doUpdateRightPane,
    doToggleCollapsePane,
  };
};
