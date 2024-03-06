<script setup lang="ts">
import { onKeyStroke } from "@vueuse/core";
import { onMounted, ref } from "vue";

import { useMainCodeEditor } from "@/editor";
import { getScriptingService, type NodeSettings } from "@/scripting-service";

// Props
interface Props {
  language: string;
  fileName: string;
  toSettings?: (settings: NodeSettings) => NodeSettings;
  dropEventHandler?: (event: DragEvent) => void;
}

const props = withDefaults(defineProps<Props>(), {
  showControlBar: true,
  paneSizes: () => ({
    left: 0,
    right: 0,
    bottom: 0,
  }),
  toSettings: (settings: NodeSettings) => settings,
  dropEventHandler: () => {},
});

const editorContainer = ref<HTMLDivElement>();
const codeEditorState = useMainCodeEditor({
  container: editorContainer,
  language: props.language,
  fileName: props.fileName,
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
  <div ref="editorContainer" @drop="dropEventHandler" />
</template>
