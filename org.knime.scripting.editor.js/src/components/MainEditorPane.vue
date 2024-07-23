<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useMainCodeEditor } from "@/editor";
import { onKeyStroke } from "@vueuse/core";
import { getScriptingService, type NodeSettings } from "@/scripting-service";
import { insertionEventHelper } from "@/components/utils/insertionEventHelper";
import { COLUMN_INSERTION_EVENT } from "@/components/InputOutputItem.vue";

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
const editorRef = ref<HTMLDivElement>();
const codeEditorState = useMainCodeEditor({
  container: editorRef,
  fileName: props.fileName,
  language: props.language,
});

insertionEventHelper
  .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
  .registerInsertionListener((event) => {
    codeEditorState.insertColumnReference(event.textToInsert);
  });

onMounted(() => {
  getScriptingService()
    .getInitialSettings()
    .then((settings) => {
      codeEditorState.setInitialText(settings.script);
      codeEditorState.editor.value?.updateOptions({
        readOnly: typeof settings.scriptUsedFlowVariable === "string",
        readOnlyMessage: {
          value: `Read-Only-Mode: The script is set by the flow variable '${settings.scriptUsedFlowVariable}'.`,
        },
      });
    });
});

onKeyStroke("Escape", () => {
  if (codeEditorState.editor.value?.hasTextFocus()) {
    (document.activeElement as HTMLElement)?.blur();
  }
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
  props.toSettings({ script: codeEditorState.text.value ?? "" }),
);
</script>

<template>
  <div class="editor-container">
    <div ref="editorRef" class="code-editor" @drop="dropEventHandler" />
  </div>
</template>

<style>
.code-editor {
  height: 100%;
}

.editor-container {
  height: 100%;
  min-height: 0;
}
</style>
