import { describe, it, expect, vi, afterEach, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import AiBar from "@/components/AiBar.vue";
import { getScriptingService } from "@/scripting-service";
import CodeEditor from "../CodeEditor.vue";
import * as monaco from "monaco-editor";
import {
  clearPromptResponseStore,
  usePromptResponseStore,
} from "../../store/ai-bar";
import LoadingIcon from "webapps-common/ui/components/LoadingIcon.vue";

vi.mock("@/scripting-service");
vi.mock("monaco-editor");

describe("AiBar", () => {
  beforeEach(() => {
    clearPromptResponseStore();
    vi.mocked(getScriptingService().supportsCodeAssistant).mockImplementation(
      () => {
        return Promise.resolve(true);
      },
    );
    vi.mocked(getScriptingService().sendToService).mockReturnValue(
      Promise.resolve(true), // logged in = true
    );
    const mockInstance = { dispose: vi.fn(), setModel: vi.fn() };
    // @ts-ignore createModel is a mock
    monaco.editor.createModel.mockReturnValue(mockInstance);
    // @ts-ignore create is also a mock
    monaco.editor.create.mockReturnValue(mockInstance);
    // @ts-ignore createDiffEditor is also a mock
    monaco.editor.createDiffEditor.mockReturnValue(mockInstance);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders chat controls if no prompt is active", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    expect(bar.find(".textarea").exists()).toBeTruthy();
    expect(bar.findComponent({ ref: "sendButton" }).exists()).toBeTruthy();
  });

  it("test aiBar success", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    (bar.vm as any).showDisclaimer = false;
    await bar.vm.$nextTick();
    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const scriptingService = getScriptingService();
    // vi mocked gives type support for mocked vi.fn()
    vi.mocked(scriptingService.sendToService).mockClear();
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

    vi.mocked(scriptingService.sendToService).mockClear();
    vi.mocked(scriptingService.sendToService).mockReturnValueOnce(
      Promise.resolve({
        code: JSON.stringify({ code: "import happy.hacking" }),
        status: "ERROR",
        error: "oh shit",
      }),
    );
    const message = "Do something!";
    const textarea = bar.find("textarea");
    textarea.setValue(message);
    const sendButton = bar.findComponent({ ref: "sendButton" });
    await sendButton.vm.$emit("click");
    const abortButton = bar.findComponent({ ref: "abortButton" });
    await abortButton.vm.$emit("click");

    expect(scriptingService.sendToService).toHaveBeenCalledTimes(2);
    expect(scriptingService.sendToService).toBeCalledWith(
      "abortSuggestCodeRequest",
    );
  });

  it("show diff editor when code suggestion is available", async () => {
    const bar = mount(AiBar);
    const editorModelInstance = { dispose: vi.fn() };
    // @ts-ignore createModel is a mock
    monaco.editor.createModel.mockReturnValue(editorModelInstance);

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

  it("show disclaimer on first startup", async () => {
    vi.mocked(getScriptingService().supportsCodeAssistant).mockImplementation(
      () => {
        return Promise.resolve(true);
      },
    );
    vi.mocked(getScriptingService().sendToService).mockReturnValue(
      Promise.resolve(true), // logged in = true
    );
    const bar = mount(AiBar);
    await flushPromises();
    (bar.vm as any).showDisclaimer = true;
    await bar.vm.$nextTick();
    const disclaimer = bar.find(".disclaimer-container");
    expect(disclaimer.exists()).toBeTruthy();
  });

  it("show login button if not logged in yet", async () => {
    vi.mocked(getScriptingService().sendToService).mockReturnValueOnce(
      Promise.resolve(false),
    );
    const bar = mount(AiBar);
    await flushPromises();
    const downloadNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);
    expect(downloadNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(downloadNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeTruthy();

    const loginButton = loginNotification?.find(".notification-button");
    expect(loginButton?.exists()).toBeTruthy();
    expect(loginButton?.text()).toBe("Login to KNIME Hub");
  });

  it("show install button if not available", async () => {
    vi.mocked(getScriptingService().supportsCodeAssistant).mockReturnValueOnce(
      Promise.resolve(false),
    );
    const bar = mount(AiBar);
    await flushPromises();
    const downloadNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);
    expect(downloadNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(downloadNotification?.isVisible()).toBeTruthy();
    expect(loginNotification?.isVisible()).toBeFalsy();

    const downloadButton = downloadNotification?.find(".notification-button");
    expect(downloadButton?.exists()).toBeTruthy();
    expect(downloadButton?.text()).toBe("Download from KNIME Hub");
  });

  it("show flow variable message if readonly", async () => {
    vi.mocked(getScriptingService().supportsCodeAssistant).mockReturnValueOnce(
      Promise.resolve(true),
    );
    vi.mocked(getScriptingService().getInitialSettings).mockReturnValueOnce(
      Promise.resolve({
        script: "my script",
        scriptUsedFlowVariable: "myVar",
      }),
    );
    const bar = mount(AiBar);
    await flushPromises();
    const downloadNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);
    expect(downloadNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeFalsy();

    const readonlyNotification = bar.findAll(".notification-bar").at(2);
    expect(readonlyNotification?.exists()).toBeTruthy();
    expect(readonlyNotification?.isVisible()).toBeTruthy();
    expect(readonlyNotification?.text()).toBe(
      "Script is overwritten by a flow variable.",
    );
  });

  it("neither install nor login buttons are visible if ai assistant is ready to be used", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    const downloadNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);
    expect(downloadNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(downloadNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeFalsy();
  });

  it("shows loading spinner in waiting state", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const scriptingService = getScriptingService();
    // vi mocked gives type support for mocked vi.fn()
    vi.mocked(scriptingService.sendToService).mockClear();
    vi.mocked(scriptingService.sendToService).mockReturnValueOnce(
      Promise.resolve({
        code: JSON.stringify({ code: "import happy.hacking" }),
        status: "SUCCESS",
      }),
    );

    expect(bar.findComponent(LoadingIcon).exists()).toBeFalsy();

    const sendButton = bar.findComponent({ ref: "sendButton" });
    await sendButton.vm.$emit("click");

    expect(bar.findComponent(LoadingIcon).exists()).toBeTruthy();
  });

  it("aborts active request if ai bar is dismissed", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    (bar.vm as any).status = "waiting";
    bar.unmount();
    expect(getScriptingService().sendToService).toHaveBeenCalledWith(
      "abortSuggestCodeRequest",
    );
  });

  it("does not abort request if ai bar is dismissed and there is no active request", async () => {
    const bar = mount(AiBar);
    await flushPromises();
    bar.unmount();
    expect(getScriptingService().sendToService).not.toHaveBeenCalledWith(
      "abortSuggestCodeRequest",
    );
  });
});
