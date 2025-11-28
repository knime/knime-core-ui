import { type Ref, reactive, ref, shallowRef, watch } from "vue";

import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import { type UseCodeEditorReturn, useMainCodeEditorStore } from "@/editor";
import type { UsageData } from "@/scripting-service";

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

// We keep track of the current inputOutputItems to be able to feed all available columns and flow variables
// to the AI for code suggestions. This gets populated from the InputOutputPane
export const currentInputOutputItems: Ref<InputOutputModel[]> = ref<
  InputOutputModel[]
>([]);

// Cache for AI usage data to avoid loading delays when opening the popup
// null = no data available (not fetched, fetch failed, or old backend)
// UsageData = valid usage data
export const usageData = ref<UsageData>({ type: "UNKNOWN" });
