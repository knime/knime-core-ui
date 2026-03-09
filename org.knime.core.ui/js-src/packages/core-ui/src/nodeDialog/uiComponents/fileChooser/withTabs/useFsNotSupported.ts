import { type Ref, computed, h, onMounted } from "vue";

import { InlineMessage } from "@knime/components";
import { KdsButton } from "@knime/kds-components";

import type { FSCategory, FileChooserValue } from "../types/FileChooserProps";

/**
 * There exist file systems in the backend that are not supported in the frontend.
 * This can happen in multiple ways:
 * - A previously with old dialog configured node is loaded with a new dialog that
 *   does not support the same file systems anymore.
 * - The user has added a file system port (making only the connected fs selectable)
 *   or has removed a connected file system.
 *
 * We show a warning message in these cases and offer to reset the file chooser to
 * an empty state.
 */
export default ({
  data,
  validCategories,
  isOverwritten,
  isConnected,
  onFsCategoryUpdate,
  clearPath,
}: {
  data: Ref<FileChooserValue>;
  validCategories: Ref<(keyof typeof FSCategory)[]>;
  isOverwritten: Ref<boolean>;
  isConnected: Ref<boolean>;
  onFsCategoryUpdate: (fsCategory: keyof typeof FSCategory) => void;
  clearPath: () => void;
}) => {
  const fsNotSupported = computed(
    () =>
      !isOverwritten.value &&
      !validCategories.value.includes(data.value.fsCategory) &&
      validCategories.value.length > 0,
  );

  /**
   * In case the user has added a file system port (making only the connected fs selectable)
   * we simply switch to the first available file system which is "CONNECTED".
   */
  onMounted(() => {
    if (!isOverwritten.value && isConnected.value) {
      onFsCategoryUpdate(validCategories.value[0]);
    }
  });

  const getFileSystemNotSupportedMessageDescription = (
    path: FileChooserValue,
  ) => {
    if (path.fsCategory === "CONNECTED") {
      return (
        "The path has been configured with a file system from an input port that is no longer available." +
        " Either add the port again or clear the path to configure using a different file system."
      );
    }
    if (path.context?.fsToString.startsWith("(HUB_SPACE,")) {
      return (
        "Selecting a path from a custom HUB Space is no longer supported in this dialog." +
        ' Consider connecting a "Space Connector" node to this node instead.'
      );
    }
    if (
      path.context?.fsToString.startsWith("(RELATIVE, knime.mountpoint,") ||
      path.context?.fsToString.startsWith("(MOUNTPOINT,")
    ) {
      return (
        "Selecting a path from a mountpoint is no longer supported in this dialog." +
        ' Consider connecting a "Mountpoint Connector" node to this node instead.'
      );
    }
    return "The selected file system is no longer selectable.";
  };

  const getNotSupportedMessageComponent = (path: FileChooserValue) =>
    h(InlineMessage, {
      variant: "warning",
      title: "The currently configured file system is no longer selectable.",
      description: getFileSystemNotSupportedMessageDescription(path),
    });

  const clearToConfigureButtonComponent = h(KdsButton, {
    size: "small",
    label: "Clear to reconfigure",
    variant: "outlined",
    onClick: () => clearPath(),
  });

  const fsNotSupportedComponent = computed(() =>
    !isConnected.value && fsNotSupported.value
      ? h(
          "div",
          {
            style: {
              display: "flex",
              flexDirection: "column",
              gap: "var(--space-4)",
            },
          },
          [
            getNotSupportedMessageComponent(data.value),
            clearToConfigureButtonComponent,
          ],
        )
      : null,
  );

  return {
    fsNotSupportedComponent,
    fsNotSupported,
  };
};
