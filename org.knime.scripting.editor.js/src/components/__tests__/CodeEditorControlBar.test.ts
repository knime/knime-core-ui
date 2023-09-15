import { describe, afterEach, beforeEach, it, vi, expect } from "vitest";

import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import { flushPromises, mount } from "@vue/test-utils";

import AiBar from "../AiBar.vue";
import { getScriptingService } from "@/scripting-service";

vi.mock("@/scripting-service");

describe("CodeEditorControlBar", () => {
  beforeEach(() => {
    vi.mocked(getScriptingService().inputsAvailable).mockImplementation(() => {
      return Promise.resolve(true);
    });
    vi.mocked(getScriptingService().supportsCodeAssistant).mockImplementation(
      () => {
        return Promise.resolve(true);
      },
    );
  });

  afterEach(() => {
    vi.clearAllMocks();
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
    vi.mocked(getScriptingService().inputsAvailable).mockImplementation(() => {
      return Promise.resolve(false);
    });

    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    expect(button.props().disabled).toBeTruthy();
  });

  it("test aiButton is enabled even if code assistance is not available", async () => {
    vi.mocked(getScriptingService().supportsCodeAssistant).mockImplementation(
      () => {
        return Promise.resolve(false);
      },
    );

    const wrapper = mount(CodeEditorControlBar);
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    expect(button.props().disabled).toBeFalsy();
  });
});
