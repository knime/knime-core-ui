import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import SettingsPage from "../SettingsPage.vue";

describe("SettingsPage", () => {
  it("renders back button", () => {
    const wrapper = mount(SettingsPage);
    expect(wrapper.find(".back-button").exists()).toBeTruthy();
  });

  it("emits close event when clicking on back button", async () => {
    const wrapper = mount(SettingsPage);
    await wrapper.find(".back-button").trigger("click");
    expect(wrapper.emitted("close-settings-page")).toBeTruthy();
  });

  it("displays slotted content", () => {
    const wrapper = mount(SettingsPage, {
      slots: {
        "settings-title": "<div class='title'>Settings title</div>",
        "settings-content": "<div class='content'>Settings content</div>",
      },
    });
    const titleSlot = wrapper.find(".title");
    expect(titleSlot.exists()).toBeTruthy();
    expect(titleSlot.text()).toContain("Settings title");
    const contentSlot = wrapper.find(".content");
    expect(contentSlot.exists()).toBeTruthy();
  });
});
