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

import { onMounted, onUnmounted, ref } from "vue";
import * as monaco from "monaco-editor";
import { getScriptingService } from "@/scripting-service";

const emit = defineEmits(["monaco-created"]);

const props = defineProps({
  language: {
    type: String,
    default: null,
  },
  fileName: {
    type: String,
    default: "main.txt",
  },
});

// Remember the model and editor so that we can dispose them when the component is unmounted
let editorModel: monaco.editor.ITextModel,
  editor: monaco.editor.IStandaloneCodeEditor;

const editorRef = ref(null);

onMounted(async () => {
  if (editorRef.value === null) {
    throw new Error(
      "Editor reference is null. This is an implementation error",
    );
  }

  const initialScript = (await getScriptingService().getInitialSettings())
    .script;

  editorModel = monaco.editor.createModel(
    initialScript,
    props.language,
    monaco.Uri.parse(`inmemory://${props.fileName}`),
  );

  editor = monaco.editor.create(editorRef.value as HTMLElement, {
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

onUnmounted(() => {
  if (typeof editorModel !== "undefined") {
    editorModel.dispose();
  }
  if (typeof editor !== "undefined") {
    editor.dispose();
  }
});
</script>

<template>
  <div ref="editorRef" class="code-editor" />
</template>

<style lang="postcss" scoped>
.code-editor {
  height: calc(100% - var(--controls-height));
}
</style>

<style lang="postcss">
.monaco-hover {
  & h1 {
    font-size: 1.5em;
  }

  & h2 {
    font-size: 1.3em;
  }

  & h3 {
    font-size: 1.1em;
  }

  & h4 {
    font-size: 1em;
  }

  & h5 {
    font-size: 0.9em;
  }

  & h6 {
    font-size: 0.8em;
  }
}
</style>
