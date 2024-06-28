import { vi } from "vitest";
import { ref, shallowRef, watch } from "vue";

export const mainEditorStore = shallowRef({
  text: ref(""),
  setInitialText: vi.fn((text) => {
    mainEditorStore.value.text.value = text;
  }),
  editorModel: {
    pushEditOperations: vi.fn((_, requiredEdits) => {
      mainEditorStore.value.text.value += requiredEdits[0].text;
    }),
  },
  insertColumnReference: vi.fn((text, requiredImport) => {
    if (requiredImport) {
      mainEditorStore.value.text.value = `${requiredImport}\n${mainEditorStore.value.text.value}\n${text}`;
    } else {
      mainEditorStore.value.text.value = `${mainEditorStore.value.text.value}\n${text}`;
    }
  }),
  insertText: vi.fn((text) => {
    mainEditorStore.value.text.value += text;
  }),
  editor: shallowRef({
    onDidChangeModelContent: vi.fn((callback: any) => {
      const unwatch = watch(
        () => mainEditorStore.value.text.value,
        () => {
          unwatch();
          callback();
        },
      );
    }),
    updateOptions: vi.fn(() => {}),
  }),
});

export const editorStore = shallowRef({
  text: ref(""),
  setInitialText: vi.fn((text) => {
    editorStore.value.text.value = text;
  }),
  editorModel: "myNonMainEditorModel",
});

export const diffEditorState = {
  modifiedText: ref(""),
  setInitialModifiedText: vi.fn((text) => {
    diffEditorState.modifiedText.value = text;
  }),
};

export const useMainCodeEditorStore = vi.fn(() => mainEditorStore);
export const useMainCodeEditor = vi.fn(() => mainEditorStore.value);
export const useDiffEditor = vi.fn(() => diffEditorState);
export const useCodeEditor = vi.fn(() => editorStore.value);

export default {
  useMainCodeEditorStore,
  useMainCodeEditor,
  useCodeEditor,
  useDiffEditor,
};
