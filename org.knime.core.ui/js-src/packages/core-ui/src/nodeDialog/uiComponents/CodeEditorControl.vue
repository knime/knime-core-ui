<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from "vue";
import * as monaco from "monaco-editor";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

const props = defineProps<VueControlPropsForLabelContent<string>>();

const container = ref<HTMLDivElement>();
let editor: monaco.editor.IStandaloneCodeEditor | undefined;

const language = props.control.uischema.options?.language ?? "";

onMounted(() => {
  if (!container.value) {
    return;
  }
  editor = monaco.editor.create(container.value, {
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

  editor.onDidChangeModelContent(() => {
    const value = editor?.getModel()?.getValue() ?? "";
    if (value !== props.control.data) {
      props.changeValue(value);
    }
  });
});

watch(
  () => props.disabled,
  (disabled) => {
    editor?.updateOptions({ readOnly: disabled });
  },
);

watch(
  () => props.control.data,
  (newValue) => {
    if (editor && newValue !== editor.getModel()?.getValue()) {
      editor.getModel()?.setValue(newValue ?? "");
    }
  },
);

onUnmounted(() => {
  editor?.dispose();
  editor = undefined;
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
