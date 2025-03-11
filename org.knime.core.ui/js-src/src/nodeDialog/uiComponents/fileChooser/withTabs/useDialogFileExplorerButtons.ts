import { type Ref, computed, onMounted, watch } from "vue";

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
  selectionMode: Ref<"FILE" | "FOLDER">;
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
    applyText.value = "Choose";
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
      if (!newSelectedItem) {
        goIntoFolderButtonDisabled.value = true;
        applyButtonDisabled.value = mode === "FILE" || isRoot;
      } else if (newSelectedItem.selectionType === "FILE") {
        applyButtonDisabled.value = mode !== "FILE";
        goIntoFolderButtonDisabled.value = true;
      } else if (newSelectedItem.selectionType === "FOLDER") {
        applyButtonDisabled.value = mode !== "FOLDER";
        goIntoFolderButtonDisabled.value = false;
      }
    },
    { immediate: true },
  );

  return {
    clickOutsideExceptions,
    goIntoFolderButtonShown,
  };
};
