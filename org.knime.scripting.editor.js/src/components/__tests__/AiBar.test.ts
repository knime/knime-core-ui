import { describe, it, expect, vi, afterEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import AiBar from "@/components/AiBar.vue";
import { getScriptingService } from "@/scripting-service";
import CodeEditor from "../CodeEditor.vue";
import * as monaco from "monaco-editor";
import { beforeEach } from "node:test";
import { clearPromptResponseStore, usePromptResponseStore } from "@/store";

vi.mock("@/scripting-service");
vi.mock("monaco-editor");

describe("AiBar", () => {
  beforeEach(() => {
    clearPromptResponseStore();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders chat controls if no prompt is active", () => {
    const bar = mount(AiBar);
    expect(bar.find(".textarea").exists()).toBeTruthy();
    expect(bar.findComponent({ ref: "sendButton" }).exists()).toBeTruthy();
  });

  it("test aiBar success", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const scriptingService = getScriptingService();
    // vi mocked gives type support for mocked vi.fn()
    vi.mocked(scriptingService.sendToService).mockReturnValueOnce(
      Promise.resolve({
        code: JSON.stringify({ code: "import happy.hacking" }),
        status: "SUCCESS",
      }),
    );
    // click Send Button
    const sendButton = bar.findComponent({ ref: "sendButton" });
    sendButton.vm.$emit("click");

    // expect scripting service to be called with
    expect(scriptingService.sendToService).toHaveBeenCalledOnce();
    expect(scriptingService.sendToService).toBeCalledWith("suggestCode", [
      message,
      undefined,
    ]);
  });

  it("test aiBar with empty input disables button", async () => {
    const bar = mount(AiBar);
    await flushPromises();

    // click Send Button
    const sendButton = bar.findComponent({ ref: "sendButton" });

    // @ts-ignore
    expect(sendButton.isDisabled).toBeTruthy();
  });

  it("test aiBar abort request", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    const scriptingService = getScriptingService();

    vi.mocked(scriptingService.sendToService).mockReturnValueOnce(
      Promise.resolve({
        code: JSON.stringify({ code: "import happy.hacking" }),
        status: "ERROR",
        error: "oh shit",
      }),
    );
    // click Send Button
    const abortButton = bar.findComponent({ ref: "abortButton" });
    abortButton.vm.$emit("click");
    await flushPromises();

    expect(scriptingService.sendToService).toHaveBeenCalledOnce();
    expect(scriptingService.sendToService).toBeCalledWith("abortRequest");
  });

  it("show diff editor when code suggestion is available", async () => {
    const bar = mount(AiBar);
    const diffEditor = bar.find(".diff-editor-container");
    expect(diffEditor.exists()).toBeFalsy();
    await (bar.vm as any).handleCodeSuggestion({
      code: JSON.stringify({ code: "some code" }),
      status: "SUCCESS",
    });
    await flushPromises();
    expect(monaco.editor.createDiffEditor).toHaveBeenCalled();
    expect(bar.findComponent(CodeEditor).exists()).toBeTruthy();
  });

  it("show diff editor when previous prompt is available", async () => {
    usePromptResponseStore().promptResponse = {
      message: { role: "reply", content: "blah" },
      suggestedCode: "code",
    };
    const bar = mount(AiBar);
    await flushPromises();
    const diffEditor = bar.find(".diff-editor-container");
    expect(diffEditor.exists()).toBeTruthy();
    expect(monaco.editor.createDiffEditor).toHaveBeenCalled();
    expect(bar.findComponent(CodeEditor).exists()).toBeTruthy();
  });
});
