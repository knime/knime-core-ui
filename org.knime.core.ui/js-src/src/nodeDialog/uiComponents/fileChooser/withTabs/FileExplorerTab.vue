<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import DialogFileExplorer, {
  type DialogFileExplorerProps,
  type SelectedItem,
} from "../DialogFileExplorer.vue";
import { useApplyButton } from "../settingsSubPanel";
import { GO_INTO_FOLDER_INJECTION_KEY } from "../settingsSubPanel/SettingsSubPanelForFileChooser.vue";

export type FileExplorerTabProps = Omit<
  DialogFileExplorerProps,
  "clickOutsideException" | "openFileByExplorer"
>;

const props = withDefaults(defineProps<FileExplorerTabProps>(), {
  initialFilePath: "",
  isWriter: false,
  filteredExtensions: () => [],
  appendedExtension: null,
  spacePath: "",
  allowMultiSelection: false,
});

const emit = defineEmits<{
  chooseItem: [
    /**
     * The full path of the chosen file
     */
    filePath: string,
  ];
}>();

const explorer = ref<typeof DialogFileExplorer | null>(null);
const chooseSelectedItem = () => explorer.value?.chooseSelectedItem();

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
} = useApplyButton(GO_INTO_FOLDER_INJECTION_KEY) ?? {
  element: ref(null),
  disabled: ref(true),
  onApply: ref(() => {}),
  hidden: ref(false),
};

onMounted(() => {
  applyText.value =
    props.selectionMode === "FILE" ? "Choose File" : "Choose Folder";
  applyButtonDisabled.value = true;
  onApply.value = chooseSelectedItem;

  goIntoFolderButtonDisabled.value = true;
  onGoIntoFolderButtonClicked.value = () =>
    explorer.value?.goIntoSelectedFolder();
});

const clickOutsideExceptions = computed(() => [
  applyButton,
  goIntoFolderButton,
]);

const onChooseItem = (name: string) => {
  emit("chooseItem", name);
};

const selectedItem = ref<SelectedItem>(null);

watch(selectedItem, (newSelectedItem) => {
  if (!newSelectedItem) {
    applyButtonDisabled.value = true;
    goIntoFolderButtonDisabled.value = true;
  } else if (newSelectedItem.selectionType === "FILE") {
    applyButtonDisabled.value = props.selectionMode !== "FILE";
    goIntoFolderButtonDisabled.value = true;
  } else if (newSelectedItem.selectionType === "FOLDER") {
    applyButtonDisabled.value = props.selectionMode !== "FOLDER";
    goIntoFolderButtonDisabled.value = false;
  }
});
</script>

<template>
  <div class="wrapper">
    <DialogFileExplorer
      ref="explorer"
      v-bind="props"
      v-model:selected-item="selectedItem"
      :click-outside-exceptions="clickOutsideExceptions"
      :selection-mode="selectionMode"
      @choose-item="onChooseItem"
    />
  </div>
</template>

<style scoped lang="postcss">
.wrapper {
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: space-between;

  & .button-wrapper {
    display: flex;
    flex-flow: row wrap;
    justify-content: space-between;
    padding: 10px 0;
  }
}
</style>
