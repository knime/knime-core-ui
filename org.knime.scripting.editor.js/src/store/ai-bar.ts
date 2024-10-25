import { reactive, ref, shallowRef, watch } from "vue";

import { type UseCodeEditorReturn, useMainCodeEditorStore } from "@/editor";

export interface Message {
  role: "reply" | "request";
  content: string;
}

export type PromptResponse = {
  suggestedCode: string;
  message: Message;
};

export type PromptResponseStore = {
  promptResponse?: PromptResponse;
};

const promptResponseStore: PromptResponseStore = reactive<PromptResponseStore>(
  {},
);

export const usePromptResponseStore = (): PromptResponseStore => {
  return promptResponseStore;
};

export const clearPromptResponseStore = (): void => {
  if (typeof promptResponseStore.promptResponse !== "undefined") {
    delete promptResponseStore.promptResponse;
  }
};

// Whether the disclaimer needs to be shown to the user.
// This is part of the store so it is only shown the first time the user
// opens the AI bar while the script editor is open.
export const showDisclaimer = ref<boolean>(true);

export const activeEditorStore = shallowRef<UseCodeEditorReturn>();

export const setActiveEditorStoreForAi = (
  store: UseCodeEditorReturn | undefined,
): void => {
  activeEditorStore.value = store;
};

// By default set the editor state to the main editor (this only happens if a main editor is open)
watch(() => useMainCodeEditorStore().value, setActiveEditorStoreForAi);
