import { ref } from "vue";

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
