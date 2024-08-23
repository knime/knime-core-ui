<script setup lang="ts">
import editor from "@/editor";
import { activeEditorStore, usePromptResponseStore } from "@/store/ai-bar";
import { ref, watch } from "vue";
import ExportIcon from "@knime/styles/img/icons/export.svg";
import { Button } from "@knime/components";

const emit =
  defineEmits<(e: "accept-suggestion", acceptedCode: string) => void>();

const diffEditorContainer = ref<HTMLDivElement>();
const diffEditorState = editor.useDiffEditor({
  container: diffEditorContainer,
  originalModel: activeEditorStore.value!.editorModel,
  modifiedFileName: "ai-suggestion",
});

const promptResponseStore = usePromptResponseStore();
watch(
  () => promptResponseStore.promptResponse?.suggestedCode,
  (newSuggestion) => {
    if (typeof newSuggestion !== "undefined") {
      diffEditorState.setInitialModifiedText(newSuggestion);
    }
  },
  { immediate: true },
);
const acceptSuggestion = () => {
  emit("accept-suggestion", diffEditorState.modifiedText.value);
};
</script>

<template>
  <div class="suggestion-container">
    <div ref="diffEditorContainer" class="diff-editor" />
    <div class="accept-decline-buttons">
      <Button with-border compact @click="acceptSuggestion">
        <ExportIcon /> Insert
      </Button>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.suggestion-container {
  display: flex;
  flex-direction: column;
  position: relative;

  & .diff-editor {
    flex-grow: 1;
    overflow: hidden;
    border-radius: var(--ai-bar-corner-radius);
    position: absolute;
    inset: 0;
  }

  & .accept-decline-buttons {
    position: absolute;
    bottom: 15px;
    right: 15px;

    & .button {
      float: right;
      margin-top: var(--ai-bar-margin);
    }
  }
}
</style>
