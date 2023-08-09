import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import HeaderBar from "../HeaderBar.vue";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import MenuIcon from "webapps-common/ui/assets/img/icons/menu-options.svg";

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
      wrapper.vm.menuItemClicked(event as any, item);
      await wrapper.vm.$nextTick();
      expect(wrapper.emitted("menu-item-click")).toBeTruthy();
      expect(wrapper.emitted("menu-item-click")![0][0]).toContain({
        event,
        item,
      });
    });
  });
});
