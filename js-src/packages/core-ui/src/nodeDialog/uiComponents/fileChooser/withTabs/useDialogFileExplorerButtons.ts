import { type Ref, computed, onMounted, watch } from "vue";

import type { FileSelectionMode } from "@/nodeDialog/types/FileChooserUiSchema";
import type { SelectedItem } from "../DialogFileExplorer.vue";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../settingsSubPanel/SettingsSubPanelForFileChooser.vue";
import { useApplyButton } from "../settingsSubPanel/useApplyButton";

export const useDialogFileExplorerButtons = ({
  actions: { chooseSelectedItem, goIntoSelectedFolder },
  selectionMode,
  selectedItem,
  isRootParent,
}: {
  actions: {
    chooseSelectedItem: () => Promise<void>;
    goIntoSelectedFolder: () => Promise<void>;
  };
  selectionMode: Ref<FileSelectionMode>;
  selectedItem: Ref<SelectedItem | null>;
  isRootParent: Ref<boolean>;
}) => {
  const {
    element: applyButton,
    disabled: applyButtonDisabled,
    text: applyText,
    onApply,
  } = useApplyButton();

  const {
    element: goIntoFolderButton,
    disabled: goIntoFolderButtonDisabled,
    onApply: onGoIntoFolderButtonClicked,
    shown: goIntoFolderButtonShown,
  } = useApplyButton(GO_INTO_FOLDER_INJECTION_KEY);

  onMounted(() => {
    onApply.value = chooseSelectedItem;

    goIntoFolderButtonShown.value = true;
    goIntoFolderButtonDisabled.value = true;
    onGoIntoFolderButtonClicked.value = () => goIntoSelectedFolder();
  });

  const clickOutsideExceptions = computed(() => [
    applyButton,
    goIntoFolderButton,
  ]);

  watch(
    [selectedItem, selectionMode, isRootParent],
    ([newSelectedItem, mode, isRoot]) => {
      if (mode === "FILE") {
        applyText.value = "Choose file";
      } else if (mode === "WORKFLOW") {
        applyText.value = "Choose workflow";
      } else {
        /**
         * Adjusted to "Choose file" or "Choose" below in certain cases when
         * "FILE_OR_FOLDER" mode is active.
         */
        applyText.value = "Choose folder";
      }
      const onlyFilesAllowed = mode === "FILE" || mode === "WORKFLOW";
      const onlyFoldersAllowed = mode === "FOLDER";
      if (!newSelectedItem) {
        goIntoFolderButtonDisabled.value = true;
        applyButtonDisabled.value = onlyFilesAllowed || isRoot;
      } else if (newSelectedItem.selectionType === "FILE") {
        applyButtonDisabled.value = onlyFoldersAllowed;
        goIntoFolderButtonDisabled.value = true;
        if (mode === "FILE_OR_FOLDER") {
          applyText.value = "Choose file";
        }
      } else if (newSelectedItem.selectionType === "FOLDER") {
        applyButtonDisabled.value = onlyFilesAllowed;
        goIntoFolderButtonDisabled.value = false;
      } else if (newSelectedItem.selectionType === "FILE_OR_FOLDER") {
        // only possible if mode is FILE_OR_FOLDER and item is determined via the text input field
        applyButtonDisabled.value = false;
        goIntoFolderButtonDisabled.value = true;
        applyText.value = "Choose";
      }
    },
    { immediate: true },
  );

  return {
    clickOutsideExceptions,
    goIntoFolderButtonShown,
  };
};
