import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
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
});
