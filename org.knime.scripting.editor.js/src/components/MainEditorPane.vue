<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useMainCodeEditor } from "@/editor";
import { onKeyStroke } from "@vueuse/core";
import { getScriptingService, type NodeSettings } from "@/scripting-service";

interface Props {
  showControlBar: boolean;
  language: string;
  fileName: string;
  toSettings?: (settings: NodeSettings) => NodeSettings;
  dropEventHandler?: (event: DragEvent) => void;
}

const props = withDefaults(defineProps<Props>(), {
  toSettings: (settings: NodeSettings) => settings,
  dropEventHandler: () => {},
});

// Main editor
const editorContainer = ref<HTMLDivElement>();
const codeEditorState = useMainCodeEditor({
  container: editorContainer,
  language: props.language,
  fileName: props.fileName,
});

onKeyStroke("Escape", () => {
  if (codeEditorState.editor.value?.hasTextFocus()) {
    (document.activeElement as HTMLElement)?.blur();
  }
});

onMounted(() => {
  getScriptingService()
    .getInitialSettings()
    .then((settings) => {
      codeEditorState.setInitialText(settings.script);
    });
});

// register undo changes from outside the editor
onKeyStroke("z", (e) => {
  const key = navigator.userAgent.toLowerCase().includes("mac")
    ? e.metaKey
    : e.ctrlKey;

  if (key) {
    codeEditorState.editor.value?.trigger("window", "undo", {});
  }
});

getScriptingService().registerSettingsGetterForApply(() =>
  props.toSettings({ script: codeEditorState.text.value }),
);
</script>

<template>
  <div class="editor-container">
    <div ref="editorContainer" class="code-editor" @drop="dropEventHandler" />
  </div>
</template>

<style>
.code-editor {
  height: 100%;
}

.editor-container {
  height: 100%;
}
</style>
