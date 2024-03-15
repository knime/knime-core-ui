import { vi } from "vitest";
import { ref, shallowRef } from "vue";

export const mainEditorStore = shallowRef({
  text: ref(""),
  setInitialText: vi.fn((text) => {
    mainEditorStore.value.text.value = text;
  }),
  editorModel: "myEditorModel",
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
