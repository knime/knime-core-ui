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

  it("renders", async () => {
    const wrapper = mountSideDrawerContent();
    await flushPromises();
    const tabBar = wrapper.findComponent(TabBar);
    expect(tabBar.exists()).toBeTruthy();
    expect(tabBar.props().possibleValues).toStrictEqual([
      {
        value: "SPACE",
        label: testSpaceName,
        icon: expect.anything(),
      },
      {
        value: "EMBEDDED",
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
    await flushPromises();
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
    await flushPromises();
    expect(wrapper.findComponent(TabBar).props().modelValue).toBe("SPACE");
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
    await flushPromises();
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
    await flushPromises();
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
    let wrapper: ReturnType<typeof mountSideDrawerContent>,
      connectedFSOptionsCallbacks: Array<(value: unknown) => void>;

    beforeEach(() => {
      props.uischema.options!.connectedFSOptions = {
        portIndex: 1,
        fileSystemType: "Connected File System",
        fileSystemSpecifier: "defaultSpecifier",
      };
      props.uischema.providedOptions = ["connectedFSOptions"];

      connectedFSOptionsCallbacks = [];
      const addStateProviderListener = vi.fn((identifier, callback) => {
        if (identifier.providedOptionName === "connectedFSOptions") {
          connectedFSOptionsCallbacks.push(callback);
        }
      });

      wrapper = shallowMount(SideDrawerContent, {
        props,
        global: {
          provide: {
            addStateProviderListener,
          },
        },
      });
    });

    it("renders CONNECTED tab", async () => {
      const fsSpecifier = "myFileSystemSpecifier";
      const updatedConnectedFSOptions = {
        portIndex: 1,
        fileSystemType: "Connected File System",
        fileSystemSpecifier: fsSpecifier,
      };

      // Update connectedFSOptions via state provider - call all callbacks
      connectedFSOptionsCallbacks.forEach((callback) =>
        callback(updatedConnectedFSOptions),
      );
      await flushPromises();
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
        await flushPromises();
        await wrapper
          .findComponent(TabBar)
          .vm.$emit("update:model-value", otherFsCategory);
        expect(
          wrapper.findComponent(ConnectionPreventsTab).exists(),
        ).toBeTruthy();
      },
    );

    it("does not render TabBar until connectedFSOptions is provided", async () => {
      // Mount with connectedFSOptions in providedOptions but not yet provided
      props.modelValue.fsCategory = "LOCAL";
      props.uischema.options!.connectedFSOptions = undefined;
      props.uischema.providedOptions = ["connectedFSOptions"];

      let connectedFSOptionsCallback: ((value: unknown) => void) | undefined;
      const addStateProviderListener = vi.fn((identifier, callback) => {
        if (identifier.providedOptionName === "connectedFSOptions") {
          connectedFSOptionsCallback = callback;
        }
      });

      wrapper = shallowMount(SideDrawerContent, {
        props,
        global: {
          provide: {
            addStateProviderListener,
          },
        },
      });

      await flushPromises();

      // TabBar should not be rendered yet
      expect(wrapper.findComponent(TabBar).exists()).toBe(false);

      // Now provide connectedFSOptions
      const connectedFSOptions = {
        portIndex: 1,
        fileSystemType: "Connected File System",
        fileSystemSpecifier: "mySpecifier",
      };
      connectedFSOptionsCallback!(connectedFSOptions);
      await flushPromises();

      // TabBar should now be rendered
      expect(wrapper.findComponent(TabBar).exists()).toBe(true);
    });
  });

  it("renders TabBar immediately when connectedFSOptions is not a provided option", async () => {
    props.uischema.providedOptions = undefined;
    const wrapper = mountSideDrawerContent();
    await flushPromises();

    // TabBar should be rendered immediately
    expect(wrapper.findComponent(TabBar).exists()).toBe(true);
  });
});
