import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";

import { Button, SplitButton, SubMenu } from "@knime/components";
import DropdownIcon from "@knime/styles/img/icons/arrow-dropdown.svg";
import CircleArrow from "@knime/styles/img/icons/circle-arrow-down.svg";
import { JsonDataService } from "@knime/ui-extension-service";

import TableViewInteractive from "../../tableView/TableViewInteractive.vue";
import DeferredTableView from "../DeferredTableView.vue";

describe("DeferredTableView.vue", () => {
  const jsonDataServiceDataMock = vi
    .fn()
    .mockReturnValue('{"table": { "columnCount": "5" }}');
  const doMount = (initialData = null) => {
    JsonDataService.mockImplementation(() => ({
      initialData: vi.fn().mockResolvedValue(initialData),
      data: jsonDataServiceDataMock,
      knimeService: {},
    }));

    const wrapper = mount(DeferredTableView, {
      global: {
        provide: {
          getKnimeService: () => ({
            extensionConfig: {
              resourceInfo: { baseUrl: "http://localhost:8080/base.url/" },
            },
          }),
        },
        stubs: {
          TableViewInteractive: {
            props: {
              enableCellSelection: {
                type: Boolean,
              },
              forceHideTableSizes: {
                type: Boolean,
              },
            },
            template: "<div/>",
          },
        },
      },
    });
    return { wrapper };
  };

  it("renders default when not data is provided", () => {
    const { wrapper } = doMount();
    expect(wrapper.findComponent(SplitButton).exists()).toBeTruthy();
    expect(wrapper.findComponent(CircleArrow).exists()).toBeTruthy();
    expect(wrapper.findComponent(DropdownIcon).exists()).toBeTruthy();
    expect(wrapper.findComponent(SubMenu).exists()).toBeTruthy();
    expect(wrapper.findComponent(Button).exists()).toBeTruthy();

    expect(wrapper.findComponent(SubMenu).props()).toEqual({
      allowOverflowMainAxis: false,
      buttonTitle: "",
      compact: false,
      disabled: false,
      id: "",
      items: [
        {
          text: "100",
          value: 100,
        },
        {
          text: "1000",
          value: 1000,
        },
        {
          text: "5000",
          value: 5000,
        },
      ],
      maxMenuWidth: null,
      menuItemsProps: {},
      orientation: "left",
      positioningStrategy: "fixed",
      teleportToBody: true,
    });
  });

  it("fetches new data on click and renders table view", async () => {
    const { wrapper } = doMount();
    wrapper.findComponent(Button).find("button").trigger("click");
    await wrapper.vm.$nextTick();
    expect(jsonDataServiceDataMock).toBeCalledWith({
      method: "getTableViewInitialData",
      options: [100],
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(TableViewInteractive).exists()).toBeTruthy();
    expect(
      wrapper.findComponent(TableViewInteractive).props().enableCellSelection,
    ).toBeFalsy();
    expect(
      wrapper.findComponent(TableViewInteractive).props().forceHideTableSizes,
    ).toBeTruthy();
    expect(wrapper.findComponent(SubMenu).props()).toEqual({
      allowOverflowMainAxis: false,
      buttonTitle: "",
      compact: false,
      disabled: false,
      id: "",
      items: [
        {
          text: "Re-fetch data",
          disabled: true,
          separator: true,
        },
        {
          text: "100",
          value: 100,
        },
        {
          text: "1000",
          value: 1000,
        },
        {
          text: "5000",
          value: 5000,
        },
      ],
      maxMenuWidth: null,
      menuItemsProps: {},
      orientation: "left",
      positioningStrategy: "fixed",
      teleportToBody: true,
    });
  });

  it("fetches new data on submenu click and renders table view", async () => {
    const { wrapper } = doMount();
    wrapper
      .findComponent(SubMenu)
      .vm.$emit("item-click", null, { value: 5000 });
    await wrapper.vm.$nextTick();
    expect(jsonDataServiceDataMock).toBeCalledWith({
      method: "getTableViewInitialData",
      options: [5000],
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(TableViewInteractive).exists()).toBeTruthy();
  });

  it("refetches new data on click in table view", async () => {
    const { wrapper } = doMount();
    wrapper.findComponent(Button).find("button").trigger("click");
    await wrapper.vm.$nextTick();
    wrapper
      .findComponent(SubMenu)
      .vm.$emit("item-click", null, { value: 1000 });
    await wrapper.vm.$nextTick();
    expect(jsonDataServiceDataMock).toBeCalledWith({
      method: "getTableViewInitialData",
      options: [1000],
    });
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(TableViewInteractive).exists()).toBeTruthy();
  });
});
