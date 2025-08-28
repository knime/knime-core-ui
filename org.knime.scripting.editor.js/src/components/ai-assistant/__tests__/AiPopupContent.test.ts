import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import InfinityLoadingBar from "@/components/InfinityLoadingBar.vue";
import AiPopupContent from "@/components/ai-assistant/AiPopupContent.vue";
import AiSuggestion from "@/components/ai-assistant/AiSuggestion.vue";
import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";
import { getScriptingService } from "@/scripting-service";
import { getSettingsService } from "@/settings-service";
import {
  DEFAULT_INITIAL_SETTINGS,
  registerSettingsMock,
} from "@/settings-service-browser-mock";
import {
  clearPromptResponseStore,
  currentInputOutputItems,
  setActiveEditorStoreForAi,
  usePromptResponseStore,
} from "@/store/ai-bar";

vi.mock("@/scripting-service");
vi.mock("@/editor");
vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));
vi.mock("@/settings-service", () => ({
  getSettingsService: vi.fn(() => ({
    registerSettingsGetterForApply: vi.fn(() => Promise.resolve()),
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettings: vi.fn(registerSettingsMock),
  })),
}));

const doMount = async () => {
  const wrapper = mount(AiPopupContent);
  await flushPromises();

  return wrapper;
};

const mockSuggestCode = () => {
  const scriptingService = getScriptingService();
  // vi mocked gives type support for mocked vi.fn()
  const mockedSendToService = vi.mocked(scriptingService.sendToService);
  mockedSendToService.mockClear();
  mockedSendToService.mockReturnValueOnce(
    Promise.resolve({
      code: JSON.stringify({ code: "import happy.hacking" }),
      status: "SUCCESS",
    }),
  );
  return mockedSendToService;
};

describe("AiPopup", () => {
  beforeEach(() => {
    vi.resetModules();
    clearPromptResponseStore();
    setActiveEditorStoreForAi({
      text: ref(""),
      editorModel: "myEditorModel",
    } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders chat controls if no prompt is active", async () => {
    const bar = await doMount();
    await flushPromises();
    expect(bar.find(".textarea").exists()).toBeTruthy();
    expect(bar.findComponent({ ref: "sendButton" }).exists()).toBeTruthy();
  });

  it("test aiBar success", async () => {
    const bar = await doMount();
    await flushPromises();
    (bar.vm as any).showDisclaimer = false;
    await bar.vm.$nextTick();
    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const sendToService = mockSuggestCode();

    // click Send Button
    const sendButton = bar.findComponent({ ref: "sendButton" });
    sendButton.vm.$emit("click");

    // expect scripting service to be called with
    expect(sendToService).toHaveBeenCalledOnce();
    expect(sendToService).toBeCalledWith("suggestCode", [message, "", []]);
  });

  it("suggestCode sends current input/output items", async () => {
    const bar = await doMount();
    await flushPromises();
    (bar.vm as any).showDisclaimer = false;
    await bar.vm.$nextTick();
    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const sendToService = mockSuggestCode();

    // Set current input/output items
    const inputOutputItems = [
      { name: "input1" },
      { name: "input2" },
      { name: "output1" },
    ];
    currentInputOutputItems.value = inputOutputItems;

    // click Send Button
    const sendButton = bar.findComponent({ ref: "sendButton" });
    sendButton.vm.$emit("click");

    // expect scripting service to be called with
    expect(sendToService).toHaveBeenCalledOnce();
    expect(sendToService).toBeCalledWith("suggestCode", [
      message,
      "",
      inputOutputItems,
    ]);
  });

  it("test aiBar with empty input disables button", async () => {
    const bar = await doMount();
    await flushPromises();

    // click Send Button
    const sendButton = bar.findComponent({ ref: "sendButton" });

    expect(sendButton.element.disabled).toBeTruthy();
  });

  it("disables send button if input textarea content is longer than 3000 characters", async () => {
    const bar = await doMount();
    await flushPromises();

    const longMessage = "a".repeat(3001);
    const textarea = bar.find("textarea");
    textarea.setValue(longMessage);

    await bar.vm.$nextTick();

    const sendButton = bar.findComponent({ ref: "sendButton" });

    expect(sendButton.element.disabled).toBeTruthy();
  });

  it("test aiBar abort request", async () => {
    const bar = await doMount();
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
    const bar = await doMount();
    expect(bar.findComponent(AiSuggestion).exists()).toBeFalsy();
    await (bar.vm as any).handleCodeSuggestion({
      code: JSON.stringify({ code: "some code" }),
      status: "SUCCESS",
    });
    expect(bar.findComponent(AiSuggestion).exists()).toBeTruthy();
  });

  it("show diff editor when previous prompt is available", async () => {
    usePromptResponseStore().promptResponse = {
      message: { role: "reply", content: "blah" },
      suggestedCode: "code",
    };

    const bar = await doMount();
    expect(bar.findComponent(AiSuggestion).exists()).toBeTruthy();
  });

  it("show disclaimer on first startup", async () => {
    vi.mocked(getScriptingService().sendToService).mockReturnValue(
      Promise.resolve(true), // logged in = true
    );
    const bar = await doMount();
    await flushPromises();
    (bar.vm as any).showDisclaimer = true;
    await bar.vm.$nextTick();
    const disclaimer = bar.find(".disclaimer-container");
    expect(disclaimer.exists()).toBeTruthy();
  });

  it("show login button if not logged in yet", async () => {
    vi.mocked(getScriptingService().isLoggedIntoHub).mockResolvedValue(false);

    const bar = await doMount();
    await flushPromises();

    const disabledNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);

    expect(disabledNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(disabledNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeTruthy();

    const loginButton = loginNotification?.find(".notification-button");

    expect(loginButton?.exists()).toBeTruthy();
    expect(loginButton?.text()).toBe(
      `Login to ${DEFAULT_INITIAL_DATA.kAiConfig.hubId}`,
    );
  });

  it("shows a 'K-AI is disabled' message if K-AI is disabled", async () => {
    const scriptingService = getScriptingService();
    vi.mocked(scriptingService.isKaiEnabled).mockResolvedValue(false);

    const bar = await doMount();

    await flushPromises();

    const disabledNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);

    expect(disabledNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(disabledNotification?.isVisible()).toBeTruthy();
    expect(loginNotification?.isVisible()).toBeFalsy();
  });

  it("show flow variable message if readonly", async () => {
    vi.mocked(getSettingsService).mockReturnValue({
      getSettings: vi.fn(() =>
        Promise.resolve({
          ...DEFAULT_INITIAL_SETTINGS,
          scriptUsedFlowVariable: "myVar",
          settingsAreOverriddenByFlowVariable: true,
          script: "myScript",
        }),
      ),
      registerSettingsGetterForApply: vi.fn(() => Promise.resolve()),
      registerSettings: vi.fn(registerSettingsMock),
    });

    const bar = await doMount();
    await flushPromises();

    const disabledNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);

    expect(disabledNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeFalsy();

    const readonlyNotification = bar.findAll(".notification-bar").at(2);

    expect(readonlyNotification?.exists()).toBeTruthy();
    expect(readonlyNotification?.isVisible()).toBeTruthy();
    expect(readonlyNotification?.text()).toBe(
      "Script is overwritten by a flow variable.",
    );
  });

  it("neither 'KAI is disabled' message nor login buttons are visible if ai assistant is ready to be used", async () => {
    const bar = await doMount();
    await flushPromises();

    const disabledNotification = bar.findAll(".notification-bar").at(0);
    const loginNotification = bar.findAll(".notification-bar").at(1);

    expect(disabledNotification?.exists()).toBeTruthy();
    expect(loginNotification?.exists()).toBeTruthy();
    expect(disabledNotification?.isVisible()).toBeFalsy();
    expect(loginNotification?.isVisible()).toBeFalsy();
  });

  it("shows abort button and loading bar in waiting state", async () => {
    const bar = await doMount();
    await flushPromises();

    const message = "Do something!";
    // write to textarea
    const textarea = bar.find("textarea");
    textarea.setValue(message);

    const scriptingService = getScriptingService();
    vi.mocked(scriptingService.isKaiEnabled).mockResolvedValue(false);
    // vi mocked gives type support for mocked vi.fn()
    vi.mocked(scriptingService.sendToService).mockClear();
    vi.mocked(scriptingService.sendToService).mockReturnValueOnce(
      Promise.resolve({
        code: JSON.stringify({ code: "import happy.hacking" }),
        status: "SUCCESS",
      }),
    );

    expect(bar.findComponent(InfinityLoadingBar).exists()).toBeFalsy();
    expect(bar.find(".abort-button").exists()).toBeFalsy();

    const sendButton = bar.findComponent({ ref: "sendButton" });
    await sendButton.vm.$emit("click");

    expect(bar.findComponent(InfinityLoadingBar).exists()).toBeTruthy();
    expect(bar.find(".abort-button").exists()).toBeTruthy();
  });

  it("aborts active request if ai bar is dismissed", async () => {
    const bar = await doMount();
    await flushPromises();

    (bar.vm as any).status = "waiting";
    bar.unmount();

    const scriptingService = getScriptingService();
    expect(scriptingService.sendToService).toHaveBeenCalledWith(
      "abortSuggestCodeRequest",
    );
  });

  it("does not abort request if ai bar is dismissed and there is no active request", async () => {
    const bar = await doMount();
    await flushPromises();

    bar.unmount();

    const scriptingService = getScriptingService();
    expect(scriptingService.sendToService).not.toHaveBeenCalledWith(
      "abortSuggestCodeRequest",
    );
  });

  describe("usage limit functionality", () => {
    it("fetches initial usage data when user is logged in", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: 100,
        used: 25,
      });

      await doMount();
      await flushPromises();

      expect(scriptingService.getAiUsage).toHaveBeenCalledOnce();
    });

    it("does not fetch usage data when user is not logged in", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.isLoggedIntoHub).mockResolvedValue(false);

      await doMount();
      await flushPromises();

      expect(scriptingService.getAiUsage).not.toHaveBeenCalled();
    });

    it("displays usage counter for regular users within limit", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: 100,
        used: 25,
      });

      const bar = await doMount();
      await flushPromises();

      const usageCounter = bar.find(".usage-counter");
      expect(usageCounter.exists()).toBeTruthy();
      expect(usageCounter.text()).toBe("25/100 monthly interactions");
    });

    it("hides usage counter for pro users (null limit)", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: null,
        used: 500,
      });

      const bar = await doMount();
      await flushPromises();

      const usageCounter = bar.find(".usage-counter");
      expect(usageCounter.exists()).toBeFalsy();
    });

    it("shows limit exceeded message when usage exceeds limit", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: 100,
        used: 100,
      });

      const bar = await doMount();
      await flushPromises();

      const limitMessage = bar.find(".limit-exceeded-message");
      expect(limitMessage.exists()).toBeTruthy();
      expect(limitMessage.text()).toContain(
        "All free monthly AI interactions used",
      );
    });

    it("hides text input when limit is exceeded", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: 100,
        used: 100,
      });

      const bar = await doMount();
      await flushPromises();

      const textarea = bar.find(".textarea");
      expect(textarea.exists()).toBeFalsy();
    });

    it("updates usage data from AI response", async () => {
      const bar = await doMount();
      await flushPromises();

      await (bar.vm as any).handleCodeSuggestion({
        code: JSON.stringify({
          code: "some code",
          interactionId: "test-id",
          usage: { limit: 100, used: 50 },
        }),
        status: "SUCCESS",
      });

      const usageCounter = bar.find(".usage-counter");
      expect(usageCounter.text()).toBe("50/100 monthly interactions");
    });

    it("shows usage disclaimer for all users", async () => {
      const bar = await doMount();
      await flushPromises();

      const disclaimer = bar.find(".usage-disclaimer");
      expect(disclaimer.exists()).toBeTruthy();
      expect(disclaimer.text().trim()).toBe("K-AI can make mistakes");
    });

    it("fetches usage data after successful login", async () => {
      const scriptingService = getScriptingService();
      vi.mocked(scriptingService.getAiUsage).mockResolvedValue({
        limit: 100,
        used: 10,
      });

      const bar = await doMount();
      await flushPromises();

      // Simulate login event
      await (bar.vm as any).handleLoginStatus(true);

      expect(scriptingService.getAiUsage).toHaveBeenCalledTimes(2); // Once on mount, once on login
    });

    it("calculates days left in month correctly", async () => {
      const bar = await doMount();

      // Mock date to January 15th, 2024
      const mockDate = new Date("2024-01-15");
      const daysLeft = (bar.vm as any).getDaysLeftInMonth(mockDate);

      // January has 31 days, so 31 - 15 = 16 days left
      expect(daysLeft).toBe(16);
    });
  });
});
