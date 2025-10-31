import { ref } from "vue";
import { get } from "lodash-es";

import type { SettingsData } from "../../types/SettingsData";

/**
 * A composable for accessing and manipulating the current data of the node dialog.
 */
export default () => {
  const currentData = ref<SettingsData | null>(null);
  const initialData = ref<SettingsData | null>(null);

  /**
   * Intended to be used initially and for updates emitted by JSONForms only.
   * If the current data should be manipulated, call getCurrentData instead.
   *
   * @param newData
   * @param path if provided, the new data are set at this path.
   */
  const setCurrentData = (newData: SettingsData) => {
    currentData.value = newData;
  };

  /**
   * Sets the initial data that will be used as the baseline for dirty state tracking.
   * This should be called once when the dialog is initialized, before any updates are applied.
   *
   * @param newData
   */
  const setInitialData = (newData: SettingsData) => {
    initialData.value = newData;
  };

  const getCurrentData = () => {
    if (currentData.value === null) {
      throw new Error("No node dialog data are set.");
    }
    return currentData.value;
  };

  /**
   * Gets the initial value at the specified path from the initial data.
   * This is used for dirty state tracking to compare against the original value,
   * not the value that may have been modified by initialUpdates.
   *
   * @param path - The path to the value in the data object
   * @returns The initial value at the path, or undefined if not found
   */
  const getInitialValue = <T>(path: string): T | undefined => {
    if (initialData.value === null) {
      throw new Error("No initial data are set.");
    }
    return <T>get(initialData.value, path);
  };

  return {
    setCurrentData,
    setInitialData,
    getCurrentData,
    getInitialValue,
  };
};
