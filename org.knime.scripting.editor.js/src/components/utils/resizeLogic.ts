import { computed, reactive } from "vue";
import type { UseElementBoundingReturn } from "@vueuse/core";
import {
  MIN_WIDTH_FOR_DISPLAYING_PANES,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
  type PaneSizes,
} from "@/components/utils/paneSizes";

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

  const collapseAllPanes = computed(
    () => rootSplitPaneRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_PANES,
  );
  const collapseLeftPane = computed(
    () => rootSplitPaneRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  );
  const showButtonText = computed(
    () => editorSplitPaneRef.width.value > MIN_WIDTH_FOR_SHOWING_BUTTON_TEXT,
  );
  const minRatioOfRightPaneInPercent = computed(
    () =>
      (rightPaneMinimumWidthInPixel /
        ((rootSplitPaneRef.width.value * (100 - largeModePaneSizes.left)) /
          100)) *
      100,
  );

  const currentPaneSizes = computed(() => {
    if (collapseAllPanes.value) {
      return {
        left: 0,
        right: 0,
        bottom: 0,
      };
    } else if (collapseLeftPane.value) {
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
    largeModePaneSizes[pane] = size;

    if (shouldUpdatePreviousPaneSize) {
      updatePreviousPaneSize(pane);
    }
  };
  const updateRightPane = (size: number) => {
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

    resizePane(newRightPaneSize, "right", false);
  };
  const collapsePane = (pane: keyof PaneSizes) => {
    const newSize =
      currentPaneSizes.value[pane] === 0 ? previousLargeModePaneSizes[pane] : 0;
    if (pane === "left") {
      updateRightPane(newSize);
    }
    resizePane(newSize, pane);
  };

  return {
    collapseAllPanes,
    collapseLeftPane,
    showButtonText,
    minRatioOfRightPaneInPercent,
    currentPaneSizes,
    usedMainPaneSize,
    usedHorizontalCodeEditorPaneSize,
    usedVerticalCodeEditorPaneSize,
    isLeftPaneCollapsed,
    isRightPaneCollapsed,
    isBottomPaneCollapsed,
    updatePreviousPaneSize,
    resizePane,
    updateRightPane,
    collapsePane,
  };
};
