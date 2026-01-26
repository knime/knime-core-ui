import { type Ref, computed, onMounted, watch } from "vue";

import type { FileSelectionMode } from "@/nodeDialog/types/FileChooserUiSchema";
import type { SelectedItem } from "../DialogFileExplorer.vue";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../settingsSubPanel/SettingsSubPanelForFileChooser.vue";
import { useApplyButton } from "../settingsSubPanel/useApplyButton";

export const useDialogFileExplorerButtons = ({
  actions: { chooseItem, goIntoSelectedFolder },
  selectionMode,
  selectedItem,
  toBeChosenItem,
  isRootParent,
}: {
  actions: {
    chooseItem: () => Promise<void>;
    goIntoSelectedFolder: () => Promise<void>;
  };
  selectionMode: Ref<FileSelectionMode>;
  selectedItem: Ref<SelectedItem | null>;
  toBeChosenItem: Ref<SelectedItem | null>;
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
    onApply.value = chooseItem;

    goIntoFolderButtonShown.value = true;
    goIntoFolderButtonDisabled.value = true;
    onGoIntoFolderButtonClicked.value = () => goIntoSelectedFolder();
  });

  const clickOutsideExceptions = computed(() => [
    applyButton,
    goIntoFolderButton,
  ]);

  watch(
    [selectedItem, selectionMode, isRootParent, toBeChosenItem],
    ([newSelectedItem, mode, isRoot, newToBeChosenItem]) => {
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

      if (!newToBeChosenItem) {
        applyButtonDisabled.value = onlyFilesAllowed || isRoot;
      } else if (newToBeChosenItem.selectionType === "FILE") {
        applyButtonDisabled.value = onlyFoldersAllowed;
        if (mode === "FILE_OR_FOLDER") {
          applyText.value = "Choose file";
        }
      } else if (newToBeChosenItem.selectionType === "FOLDER") {
        applyButtonDisabled.value = onlyFilesAllowed;
      } else if (newToBeChosenItem.selectionType === "FILE_OR_FOLDER") {
        // only possible if mode is FILE_OR_FOLDER and item is determined via the text input field
        applyButtonDisabled.value = false;
        applyText.value = "Choose";
      }

      if (!newSelectedItem) {
        goIntoFolderButtonDisabled.value = true;
      } else if (newSelectedItem.selectionType === "FILE") {
        goIntoFolderButtonDisabled.value = true;
      } else if (newSelectedItem.selectionType === "FOLDER") {
        goIntoFolderButtonDisabled.value = false;
      } else if (newSelectedItem.selectionType === "FILE_OR_FOLDER") {
        goIntoFolderButtonDisabled.value = true;
      }
    },
    { immediate: true },
  );

  return {
    clickOutsideExceptions,
    goIntoFolderButtonShown,
  };
};
