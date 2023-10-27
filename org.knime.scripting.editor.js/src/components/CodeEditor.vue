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
  initialScript: {
    type: String,
    default: null,
  },
  diffScript: {
    type: String,
    default: null,
  },
  loadScriptFromService: {
    type: Boolean,
    default: true,
  },
});

// Remember the model and editor so that we can dispose them when the component is unmounted
let editorModel: monaco.editor.ITextModel,
  diffEditorModel: monaco.editor.ITextModel,
  editor:
    | monaco.editor.IStandaloneCodeEditor
    | monaco.editor.IStandaloneDiffEditor;

const editorRef = ref(null);

onMounted(async () => {
  if (editorRef.value === null) {
    throw new Error(
      "Editor reference is null. This is an implementation error",
    );
  }

  const initialScript =
    props.initialScript ??
    getScriptingService().getScript() ??
    (await getScriptingService().getInitialSettings()).script;

  editorModel = monaco.editor.createModel(
    initialScript,
    props.language,
    monaco.Uri.parse(`inmemory://model/${props.fileName}`),
  );

  const editorSettings = {
    minimap: { enabled: false },
    automaticLayout: true,
    glyphMargin: false,
    lightbulb: {
      enabled: true,
    },
    scrollBeyondLastLine: true,
    fixedOverflowWidgets: true,
    suggest: { showWords: false }, // Disable word suggestions - better suggestions are provided by the language server
    fontFamily: '"Roboto Mono", serif',
    lineNumbersMinChars: 3,
    lineDecorationsWidth: "0.0ch",
  };

  if (props.diffScript === null) {
    editor = monaco.editor.create(editorRef.value as HTMLElement, {
      model: editorModel,
      ...editorSettings,
    });
  } else {
    diffEditorModel = monaco.editor.createModel(
      props.diffScript,
      props.language,
    );
    editor = monaco.editor.createDiffEditor(editorRef.value as HTMLElement, {
      originalEditable: false,
      ...editorSettings,
    });
    editor.setModel({
      original: editorModel,
      modified: diffEditorModel,
    });
  }
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
  if (typeof diffEditorModel !== "undefined") {
    diffEditorModel.dispose();
  }
});
</script>

<template>
  <div ref="editorRef" class="code-editor" />
</template>

<style lang="postcss" scoped></style>

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
