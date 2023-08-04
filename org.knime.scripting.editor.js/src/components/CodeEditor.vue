<script lang="ts">
import { defineComponent } from "vue";
import * as monaco from "monaco-editor";

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
export default defineComponent({
  name: "CodeEditor",
  props: {
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
  },
  emits: ["monaco-created"],
  mounted() {
    // TODO(AP-19354) Make the editor configurable
    // TODO(AP-19354) Add VIM keybindings

    // Create the editor
    const editorModel = monaco.editor.createModel(
      this.initialScript,
      this.language,
      monaco.Uri.parse(`inmemory://${this.fileName}`),
    );

    const editor = monaco.editor.create(
      this.$refs.monaco_editor as HTMLElement,
      {
        model: editorModel,
        glyphMargin: false,
        lightbulb: {
          enabled: true,
        },
        minimap: { enabled: true },
        automaticLayout: true,
        scrollBeyondLastLine: true,
        fixedOverflowWidgets: true,
      },
    );

    // Notify the parent that the editor is now available
    this.$emit("monaco-created", { editor, editorModel });
  },
});
</script>

<template>
  <div ref="monaco_editor" class="code-editor" />
</template>

<style lang="postcss" scoped>
.code-editor {
  height: 100%;
}
</style>
