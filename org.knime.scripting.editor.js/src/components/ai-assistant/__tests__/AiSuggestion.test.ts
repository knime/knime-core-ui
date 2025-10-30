import { beforeEach, describe, expect, it, vi } from "vitest";
import { isRef, nextTick, ref } from "vue";
import { mount } from "@vue/test-utils";

import { Button } from "@knime/components";

import { diffEditorState } from "@/__mocks__/editor";
import { useDiffEditor } from "@/editor";
import {
  setActiveEditorStoreForAi,
  usePromptResponseStore,
} from "@/store/ai-bar";
import AiSuggestion from "../AiSuggestion.vue";

vi.mock("@/editor");

describe("AiSuggestion", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    setActiveEditorStoreForAi({
      text: ref(""),
      editorModel: "myEditorModel",
    } as any);
  });

  it("should render diff editor", () => {
    usePromptResponseStore().promptResponse = {
      message: { content: "Hello", role: "reply" },
      suggestedCode: "print('Hello')",
    };
    const wrapper = mount(AiSuggestion);
    const editorContainer = wrapper.find(".diff-editor");
    // Patch: compare ref value instead of ref object
    const callArgs = (useDiffEditor as any).mock.calls[0][0];
    expect(isRef(callArgs.container)).toBe(true);
    expect(callArgs.container.value).toBe(editorContainer.element);
    expect(callArgs.modifiedFileName).toBe("ai-suggestion");
    expect(callArgs.originalModel).toBe("myEditorModel");
  });

  it("should update diff on prompt response change", async () => {
    usePromptResponseStore().promptResponse = {
      message: { content: "Hello", role: "reply" },
      suggestedCode: "print('Hello')",
    };
    mount(AiSuggestion);
    expect(diffEditorState.modifiedText.value).toBe("print('Hello')");
    usePromptResponseStore().promptResponse = {
      message: { content: "Hello", role: "reply" },
      suggestedCode: "Another suggestion",
    };
    await nextTick();
    expect(diffEditorState.setInitialModifiedText).toHaveBeenCalledWith(
      "Another suggestion",
    );
    expect(diffEditorState.modifiedText.value).toBe("Another suggestion");
  });

  it("should render insert in editor button", () => {
    const wrapper = mount(AiSuggestion);
    const button = wrapper.findComponent(Button);
    expect(button.exists()).toBeTruthy();
    expect(button.text()).toContain("Insert");
  });

  it("should emit accept-suggestion on button click", () => {
    const wrapper = mount(AiSuggestion);
    diffEditorState.modifiedText.value = "My edited suggestion";
    const button = wrapper.findComponent(Button);
    button.vm.$emit("click");
    expect(wrapper.emitted("accept-suggestion")).toEqual([
      ["My edited suggestion"],
    ]);
  });
});
