import { ref } from "vue";

import { DialogService } from "@knime/ui-extension-service";

/**
 * Reactive reference for the display mode of the configuration panel.
 *
 * - `"small"`: The configuration panel is displayed alongside the workflow within the UI.
 * - `"large"`: The configuration panel is displayed as an overlay over the UI or in a separate window.
 *
 * The initial value of `displayMode` is dynamically set based on the user's UI configuration.
 * It automatically updates whenever the display mode changes within the `DialogService`.
 */
export const displayMode = ref<"small" | "large">("small");

DialogService.getInstance().then((dialogService) => {
  // Set the initial value of displayMode
  displayMode.value = dialogService.getInitialDisplayMode();

  // Register a listener to update displayMode whenever it changes
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });
});
