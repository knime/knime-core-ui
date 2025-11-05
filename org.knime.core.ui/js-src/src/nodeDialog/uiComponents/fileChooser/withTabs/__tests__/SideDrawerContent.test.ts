import { beforeEach, describe, expect, it, vi } from "vitest";
import { shallowMount } from "@vue/test-utils";
import flushPromises from "flush-promises";

import { TabBar } from "@knime/components";

import DialogFileExplorer from "../../DialogFileExplorer.vue";
import type {
  FileChooserProps,
  FileChooserValue,
} from "../../types/FileChooserProps";
import { FSCategory } from "../../types/FileChooserProps";
import ConnectionPreventsTab from "../ConnectionPreventsTab.vue";
import SideDrawerContent from "../SideDrawerContent.vue";
import UrlTab from "../url/UrlTab.vue";

describe("SideDrawerContent.vue", () => {
  let props: FileChooserProps & {
    selectionMode: "FILE" | "FOLDER";
  };

  const testSpaceName = "testSpaceName";

  beforeEach(() => {
    props = {
      disabled: false,
      id: "myId",
      modelValue: {
        fsCategory: "relative-to-current-hubspace",
        path: "myPath",
        timeout: 1000,
      },
      uischema: {
        scope: "#/properties/some/properties/scope",
        options: {
          spaceFSOptions: {
            mountId: testSpaceName,
          },
        },
      },
      selectionMode: "FILE",
    };
  });

  const mountSideDrawerContent = () => {
    return shallowMount(SideDrawerContent, {
      props,
      global: {
        provide: {
          addStateProviderListener: vi.fn(),
        },
      },
    });
  };

  it("renders", () => {
    const wrapper = mountSideDrawerContent();
    const tabBar = wrapper.findComponent(TabBar);
    expect(tabBar.exists()).toBeTruthy();
    expect(tabBar.props().possibleValues).toStrictEqual([
      {
        value: "relative-to-current-hubspace",
        label: testSpaceName,
        icon: expect.anything(),
      },
      {
        value: "relative-to-embedded-data",
        label: "Embedded data",
        icon: expect.anything(),
      },
      {
        value: "CUSTOM_URL",
        label: "URL",
        icon: expect.anything(),
      },
    ]);
  });

  it("renders local tab", async () => {
    props.modelValue.fsCategory = "LOCAL";
    props.uischema.options!.isLocal = true;
    const wrapper = mountSideDrawerContent();
    expect(wrapper.findComponent(TabBar).props().modelValue).toBe("LOCAL");
    await flushPromises();
    const fileExplorerTab = wrapper.findComponent(DialogFileExplorer);
    expect(fileExplorerTab.exists()).toBeTruthy();
    expect(fileExplorerTab.props().backendType).toBe("local");
    const updatedPath = "updatedPath";
    await fileExplorerTab.vm.$emit("chooseItem", updatedPath);
    expect(wrapper.emitted("update:modelValue")![0][0]).toStrictEqual({
      ...props.modelValue,
      path: updatedPath,
    });
    expect(fileExplorerTab.props().breadcrumbRoot).toBeNull();
  });

  it("renders current hub space tab", async () => {
    const spacePath = "mySpacePath";
    props.uischema.options!.spaceFSOptions!.spacePath = spacePath;
    const wrapper = mountSideDrawerContent();
    expect(wrapper.findComponent(TabBar).props().modelValue).toBe(
      "relative-to-current-hubspace",
    );
    await flushPromises();
    const fileExplorerTab = wrapper.findComponent(DialogFileExplorer);
    expect(fileExplorerTab.exists()).toBeTruthy();
    expect(fileExplorerTab.props().backendType).toBe(
      "relativeToCurrentHubSpace",
    );
    expect(fileExplorerTab.props().breadcrumbRoot).toBe(spacePath);
  });

  it("renders embedded data tab", async () => {
    const wrapper = mountSideDrawerContent();
    await wrapper
      .findComponent(TabBar)
      .vm.$emit("update:model-value", "relative-to-embedded-data");
    await wrapper.setProps({
      modelValue: wrapper.emitted(
        "update:modelValue",
      )![0][0] as FileChooserValue,
    });
    const fileExplorerTab = wrapper.findComponent(DialogFileExplorer);
    expect(fileExplorerTab.exists()).toBeTruthy();
    expect(fileExplorerTab.props().backendType).toBe("embedded");
    expect(fileExplorerTab.props().breadcrumbRoot).toBe("Data");
  });

  it("renders URL tab", async () => {
    const wrapper = mountSideDrawerContent();
    await wrapper
      .findComponent(TabBar)
      .vm.$emit("update:model-value", "CUSTOM_URL");
    await wrapper.setProps({
      modelValue: wrapper.emitted(
        "update:modelValue",
      )![0][0] as FileChooserValue,
    });
    const urlTab = wrapper.findComponent(UrlTab);
    expect(urlTab.exists()).toBeTruthy();
    const updatedPath = "updatedPath";
    await urlTab.vm.$emit("update:path", updatedPath);
    expect(wrapper.emitted("update:modelValue")![1][0]).toMatchObject({
      path: updatedPath,
    });
    const updatedTimeout = 2000;
    await urlTab.vm.$emit("update:timeout", updatedTimeout);
    expect(wrapper.emitted("update:modelValue")![2][0]).toMatchObject({
      timeout: updatedTimeout,
    });
  });

  describe("when a file system port exists", () => {
    let wrapper: ReturnType<typeof mountSideDrawerContent>;

    beforeEach(() => {
      props.uischema.options!.connectedFSOptions = {
        portIndex: 1,
        fileSystemType: "Connected File System",
        fileSystemSpecifier: "defaultSpecifier",
      };
      wrapper = mountSideDrawerContent();
    });

    it("renders CONNECTED tab", async () => {
      const fsSpecifier = "myFileSystemSpecifier";
      await wrapper.setProps({
        uischema: {
          ...wrapper.props().uischema,
          options: {
            ...wrapper.props().uischema.options,
            connectedFSOptions: {
              ...wrapper.props().uischema.options!.connectedFSOptions!,
              fileSystemSpecifier: fsSpecifier,
            },
          },
        },
      });
      await wrapper
        .findComponent(TabBar)
        .vm.$emit("update:model-value", "CONNECTED");
      await wrapper.setProps({
        modelValue: wrapper.emitted(
          "update:modelValue",
        )![0][0] as FileChooserValue,
      });
      const fileExplorerTab = wrapper.findComponent(DialogFileExplorer);
      expect(fileExplorerTab.exists()).toBeTruthy();
      expect(fileExplorerTab.props().backendType).toBe("connected1");
      const updatedPath = "updatedPath";
      await fileExplorerTab.vm.$emit("chooseItem", updatedPath);
      expect(wrapper.emitted("update:modelValue")![1][0]).toStrictEqual({
        path: updatedPath,
        timeout: props.modelValue.timeout,
        fsCategory: "CONNECTED",
        context: {
          fsSpecifier,
        },
      });
    });

    it.each(["CUSTOM_URL", "LOCAL", "relative-to-current-hubspace"] as const)(
      "renders ConnectionPreventsTab for all other tabs",
      async (otherFsCategory: keyof typeof FSCategory) => {
        const wrapper = mountSideDrawerContent();
        await wrapper
          .findComponent(TabBar)
          .vm.$emit("update:model-value", otherFsCategory);
        expect(
          wrapper.findComponent(ConnectionPreventsTab).exists(),
        ).toBeTruthy();
      },
    );
  });
});
