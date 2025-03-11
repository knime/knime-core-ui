<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import DialogFileExplorer, {
  type DialogFileExplorerProps,
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
  disabled: noFileSelected,
  text: applyText,
  onApply,
} = useApplyButton();

const {
  element: otherApplyButton,
  disabled: otherNoFileSelected,
  text: otherApplyText,
  onApply: otherOnApply,
} = useApplyButton("goIntoSelectedFolder");

onMounted(() => {
  applyText.value = "Choose File";
  noFileSelected.value = true;
  onApply.value = openFile;

  otherOnApply.value = async () => {
    explorer.value?.goIntoSelectedFolder();
  };
  otherNoFileSelected.value = false;
});

const clickOutsideExceptions = computed(() => [applyButton, otherApplyButton]);

const onOpenFile = (name: string) => {
  emit("chooseFile", name);
};
</script>

<template>
  <div class="wrapper">
    <DialogFileExplorer
      ref="explorer"
      v-bind="props"
      :click-outside-exceptions="clickOutsideExceptions"
      :selection-mode="selectionMode"
      @choose-file="onOpenFile"
      @file-is-selected="
        (isSelected) => {
          noFileSelected = !isSelected;
        }
      "
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
