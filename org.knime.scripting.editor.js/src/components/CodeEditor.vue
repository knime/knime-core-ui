<script setup lang="ts">
/**
 * A Vue component for the Monaco editor. Use the property "initialScript" to set the text that should be loaded into
 * the editor.
 *
 * The component emits the event "monaco-created" with the parameter
 * ```json
 * {
 *   editor: monaco.editor.IStandaloneCodeEditor,
 *   editorModel: monaco.editor.ITextModel
 * }.
 * ```
 *
 * Use
 * ```js
 * editorModel.onDidChangeContent(e => {
 *   // Do something
 * })
 * ```
 * to react script changes.
 */

import { onMounted, ref } from "vue";
import * as monaco from "monaco-editor";

const emit = defineEmits(["monaco-created"]);

const props = defineProps({
  initialScript: {
    type: String,
    default: "",
  },
  language: {
    type: String,
    default: null,
  },
  fileName: {
    type: String,
    default: "main.txt",
  },
});

const editorRef = ref(null);

onMounted(() => {
  if (editorRef.value === null) {
    throw new Error(
      "Editor reference is null. This is an implementation error",
    );
  }

  const editorModel = monaco.editor.createModel(
    props.initialScript,
    props.language,
    monaco.Uri.parse(`inmemory://${props.fileName}`),
  );

  const editor = monaco.editor.create(editorRef.value as HTMLElement, {
    model: editorModel,
    glyphMargin: false,
    lightbulb: {
      enabled: true,
    },
    minimap: { enabled: true },
    automaticLayout: true,
    scrollBeyondLastLine: true,
    fixedOverflowWidgets: true,
  });

  // Notify the parent that the editor is now available
  emit("monaco-created", { editor, editorModel });
});
</script>

<template>
  <div ref="editorRef" class="code-editor" />
</template>

<style lang="postcss" scoped>
.code-editor {
  height: 100%;
}
</style>
