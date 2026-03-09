import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";

import { SubMenu } from "@knime/components";
import MenuIcon from "@knime/styles/img/icons/menu-options.svg";

import HeaderBar from "../HeaderBar.vue";

describe("HeaderBar", () => {
  const defaultProps = {
    title: "title",
    menuItems: [
      {
        text: "Setting 1",
      },
      {
        text: "Setting 2",
      },
    ],
  };

  it("renders components", () => {
    const wrapper = mount(HeaderBar, { props: defaultProps });
    expect(wrapper.findComponent(SubMenu).exists()).toBeTruthy();
    expect(wrapper.findComponent(MenuIcon).exists()).toBeTruthy();
  });

  describe("kebap menu", () => {
    it("does not show menu if there are no menu items", () => {
      const wrapper = mount(HeaderBar, {
        props: {
          ...defaultProps,
          menuItems: [],
        },
      });
      expect(wrapper.findComponent(SubMenu).exists()).toBeFalsy();
    });

    it("re-emits menu item click events", async () => {
      const wrapper = mount(HeaderBar, { props: defaultProps });
      const event = { id: "myEvent" };
      const item = { text: "myItem" };

      // Emit the event from the SubMenu component
      const subMenu = wrapper.findComponent(SubMenu);
      subMenu.vm.$emit("item-click", event, item);
      await wrapper.vm.$nextTick();

      // Check that the HeaderBar re-emitted the event
      expect(wrapper.emitted("menu-item-click")).toBeTruthy();
      expect(wrapper.emitted("menu-item-click")![0][0]).toEqual({
        event,
        item,
      });
    });
  });
});
