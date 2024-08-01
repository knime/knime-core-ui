import { describe, afterEach, it, vi, expect } from "vitest";
import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import { flushPromises, mount } from "@vue/test-utils";
import AiBar from "../ai-assistant/AiBar.vue";
import { getInitialDataService } from "@/initial-data-service";
import { beforeEach } from "node:test";
import { ref } from "vue";
import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";

vi.mock("@/scripting-service");
vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
    isInitialDataLoaded: vi.fn(() => true),
  })),
}));

describe("CodeEditorControlBar", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    vi.resetModules();
  });

  it("hides ai button if ai assistant disabled", async () => {
    vi.mocked(getInitialDataService).mockReturnValue({
      getInitialData: vi.fn(() =>
        Promise.resolve({
          ...DEFAULT_INITIAL_DATA,
          kAiConfig: {
            ...DEFAULT_INITIAL_DATA.kAiConfig,
            codeAssistantEnabled: false,
          },
        }),
      ),
      isInitialDataLoaded: vi.fn(() => ref(true)),
    });

    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();

    expect(wrapper.findComponent({ ref: "aiButton" }).exists()).toBeFalsy();
  });

  it("ai button opens ai bar", async () => {
    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();

    const button = wrapper.find(".ai-button");

    // aiBar should be turned off at mount
    expect(wrapper.findComponent(AiBar).exists()).toBeFalsy();
    await button.trigger("click");

    // then it should be visible
    expect(wrapper.findComponent(AiBar).exists()).toBeTruthy();
  });

  it("ai button closes ai bar if it is opened", async () => {
    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.find(".ai-button");

    await button.trigger("click");
    expect(wrapper.findComponent(AiBar).exists()).toBeTruthy();
    await button.trigger("click");
    expect(wrapper.findComponent(AiBar).exists()).toBeFalsy();
  });

  it("ai bar is closed on click outside of ai bar", async () => {
    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.find(".ai-button");
    await wrapper.vm.$nextTick();
    await button.trigger("click");
    expect(wrapper.findComponent(AiBar).exists()).toBeTruthy();
    window.dispatchEvent(new Event("click")); // emulate click outside
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(AiBar).exists()).toBeFalsy();
  });

  it("ai bar is not closed on click inside of ai bar", async () => {
    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.find(".ai-button");
    await wrapper.vm.$nextTick();
    await button.trigger("click");
    expect(wrapper.findComponent(AiBar).exists()).toBeTruthy();
    await wrapper.findComponent(AiBar).trigger("click");
    expect(wrapper.findComponent(AiBar).exists()).toBeTruthy();
  });

  it("test aiButton is available if inputs and code assistance are available", async () => {
    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    expect(button.props().disabled).toBeFalsy();
  });

  it("test aiButton is disabled if inputs are not available", async () => {
    vi.mocked(getInitialDataService).mockReturnValue({
      getInitialData: vi.fn(() =>
        Promise.resolve({
          ...DEFAULT_INITIAL_DATA,
          inputsAvailable: false,
        }),
      ),
      isInitialDataLoaded: vi.fn(() => ref(true)),
    });

    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    expect(button.props().disabled).toBeTruthy();
  });

  it("test aiButton is enabled even if code assistant is not installed", async () => {
    vi.mocked(getInitialDataService().getInitialData).mockResolvedValue({
      ...DEFAULT_INITIAL_DATA,
      kAiConfig: {
        ...DEFAULT_INITIAL_DATA.kAiConfig,
        codeAssistantInstalled: false,
      },
    });

    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    expect(button.props().disabled).toBeFalsy();
  });
});
