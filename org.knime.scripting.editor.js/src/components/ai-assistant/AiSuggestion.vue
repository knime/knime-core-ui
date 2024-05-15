<script setup lang="ts">
import editor from "@/editor";
import { activeEditorStore, usePromptResponseStore } from "@/store/ai-bar";
import { ref, watch } from "vue";
import ExportIcon from "webapps-common/ui/assets/img/icons/export.svg";
import Button from "webapps-common/ui/components/Button.vue";

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
        <ExportIcon /> Insert in editor
      </Button>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.suggestion-container {
  display: flex;
  flex-direction: column;

  & .diff-editor {
    flex-grow: 1;
  }

  & .accept-decline-buttons {
    & .button {
      float: right;
      margin-top: var(--ai-bar-margin);
    }
  }
}
</style>
