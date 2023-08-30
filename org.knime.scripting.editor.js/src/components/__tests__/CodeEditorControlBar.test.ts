import { describe, afterEach, it, vi, expect } from "vitest";

import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import { mount, flushPromises } from "@vue/test-utils";

import AiBar from "../AiBar.vue";

vi.mock("@/scripting-service");

describe("CodeEditorContorlBar test", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("test existence", async () => {
    // mount to dom to support isVisible
    const wrapper = mount(CodeEditorControlBar, { attachTo: document.body });
    await flushPromises();
    const button = wrapper.findComponent({ ref: "aiButton" });

    // aiBar should be turned off at mount
    const aiBar = wrapper.findComponent(AiBar);
    expect(aiBar.isVisible()).toBeFalsy();

    await button.vm.$emit("click");
    expect(button.emitted()).toHaveProperty("click");
    await wrapper.vm.$nextTick();
    await flushPromises();

    // then it should be visible
    expect(aiBar.isVisible()).toBeTruthy();
  });
});
