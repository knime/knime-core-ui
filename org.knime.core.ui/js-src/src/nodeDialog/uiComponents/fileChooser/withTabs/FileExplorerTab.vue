<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import DialogFileExplorer, {
  type DialogFileExplorerProps,
  type SelectedItem,
} from "../DialogFileExplorer.vue";
import { useApplyButton } from "../settingsSubPanel";

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
  chooseFile: [
    /**
     * The full path of the chosen file
     */
    filePath: string,
  ];
}>();

const explorer = ref<typeof DialogFileExplorer | null>(null);
const openFile = () => explorer.value?.openFile();

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
  hidden: goIntoFolderButtonHidden,
} = useApplyButton("goIntoSelectedFolder") ?? {
  element: ref(null),
  disabled: ref(true),
  onApply: ref(() => {}),
  hidden: ref(false),
};

onMounted(() => {
  applyText.value = "Choose File";
  applyButtonDisabled.value = true;
  onApply.value = openFile;

  goIntoFolderButtonDisabled.value = true;
  onGoIntoFolderButtonClicked.value = () =>
    explorer.value?.goIntoSelectedFolder();
  goIntoFolderButtonHidden.value = props.selectionMode !== "FOLDER";
});

const clickOutsideExceptions = computed(() => [
  applyButton,
  goIntoFolderButton,
]);

const onOpenFile = (name: string) => {
  emit("chooseFile", name);
};

watch(
  () => props.selectionMode,
  (newSelectionMode: DialogFileExplorerProps["selectionMode"]) => {
    goIntoFolderButtonHidden.value = newSelectionMode !== "FOLDER";
  },
);

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
      @choose-file="onOpenFile"
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
