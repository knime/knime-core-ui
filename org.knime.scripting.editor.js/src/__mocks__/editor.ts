import { vi } from "vitest";
import { ref, shallowRef } from "vue";

export const mainEditorStore = shallowRef({
  text: ref(""),
  setInitialText: vi.fn((text) => {
    mainEditorStore.value.text.value = text;
  }),
  editorModel: "myEditorModel",
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

export default {
  useMainCodeEditorStore,
  useMainCodeEditor,
  useDiffEditor,
};
