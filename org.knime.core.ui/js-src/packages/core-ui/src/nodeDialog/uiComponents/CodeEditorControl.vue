<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from "vue";
import { editor } from "monaco-editor";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

const props = defineProps<VueControlPropsForLabelContent<string>>();

const container = ref<HTMLDivElement>();
let editorInstance: editor.IStandaloneCodeEditor | undefined;

const language = props.control.uischema.options?.language ?? "";

onMounted(() => {
  if (!container.value) {
    return;
  }
  editorInstance = editor.create(container.value, {
    value: props.control.data ?? "",
    language,
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    lineNumbersMinChars: 3,
    fontFamily: '"Roboto Mono", monospace',
    fontWeight: "400",
    readOnly: props.disabled,
  });

  editorInstance.onDidChangeModelContent(() => {
    const value = editorInstance?.getModel()?.getValue() ?? "";
    if (value !== props.control.data) {
      props.changeValue(value);
    }
  });
});

watch(
  () => props.disabled,
  (disabled) => {
    editorInstance?.updateOptions({ readOnly: disabled });
  },
);

watch(
  () => props.control.data,
  (newValue) => {
    if (editorInstance && newValue !== editorInstance.getModel()?.getValue()) {
      editorInstance.getModel()?.setValue(newValue ?? "");
    }
  },
);

onUnmounted(() => {
  editorInstance?.dispose();
  editorInstance = undefined;
});
</script>

<template>
  <div :id="labelForId" ref="container" class="code-editor-container" />
</template>

<style scoped>
.code-editor-container {
  min-height: 200px;
  border: 1px solid var(--knime-stone-gray);
}
</style>
