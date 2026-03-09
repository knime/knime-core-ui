import { afterEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { shallowMount } from "@vue/test-utils";

import SideDrawerContent from "../SideDrawerContent.vue";

describe("SideDrawerContent.vue - Per-Tab Path Persistence", () => {
  const mountSideDrawerContent = (props: any) => {
    return shallowMount(SideDrawerContent, {
      props,
      global: {
        provide: {
          addStateProviderListener: vi.fn(),
        },
      },
    });
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe("initial path resolution per tab", () => {
    it("uses the initial path from modelValue only for the initially selected tab (LOCAL)", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            isLocal: true,
          },
        },
      });

      await nextTick();

      const fileExplorer = wrapper.findComponent({
        name: "DialogFileExplorer",
      });
      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().initialFilePath).toBe(
        "/initial/local/path.txt",
      );
    });

    it("uses empty initial path for non-initially-selected tabs", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            isLocal: true,
            spaceFSOptions: {
              mountId: "testSpace",
            },
          },
        },
      });

      await nextTick();

      // Initially on LOCAL tab - should have the path
      let fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().initialFilePath).toBe(
        "/initial/local/path.txt",
      );

      // Switch to SPACE tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "relative-to-current-hubspace",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      // SPACE tab should start with empty path (not the LOCAL path)
      expect(fileExplorer.props().initialFilePath).toBe("");
    });

    it("uses the initial path for SPACE tab when it is initially selected", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "relative-to-current-hubspace",
          path: "/initial/space/data.csv",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            spaceFSOptions: {
              mountId: "testSpace",
            },
          },
        },
      });

      await nextTick();

      const fileExplorer = wrapper.findComponent({
        name: "DialogFileExplorer",
      });
      expect(fileExplorer.exists()).toBeTruthy();
      expect(fileExplorer.props().initialFilePath).toBe(
        "/initial/space/data.csv",
      );
    });
  });

  describe("path persistence when switching tabs", () => {
    it("preserves the navigated location when switching from LOCAL to SPACE and back", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            isLocal: true,
            spaceFSOptions: {
              mountId: "testSpace",
            },
          },
        },
      });

      await nextTick();

      let fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().initialFilePath).toBe(
        "/initial/local/path.txt",
      );

      // Simulate navigation in LOCAL tab
      fileExplorer.vm.$emit("navigate", "/local/navigated/path");
      await nextTick();

      // Switch to SPACE tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "relative-to-current-hubspace",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().initialFilePath).toBe("");

      // Navigate in SPACE tab
      fileExplorer.vm.$emit("navigate", "/space/navigated/path");
      await nextTick();

      // Switch back to LOCAL tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/local/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      // LOCAL tab should remember the navigated path
      expect(fileExplorer.props().initialFilePath).toBe(
        "/local/navigated/path",
      );
    });

    it("preserves separate paths for multiple tab switches", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/path.txt",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            isLocal: true,
            spaceFSOptions: {
              mountId: "testSpace",
            },
          },
        },
      });

      await nextTick();

      let fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().backendType).toBe("local");
      expect(fileExplorer.props().initialFilePath).toBe("/initial/path.txt");

      // Navigate in LOCAL
      fileExplorer.vm.$emit("navigate", "/local/new/path");
      await nextTick();

      // Switch to SPACE tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "relative-to-current-hubspace",
          path: "/initial/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().backendType).toBe(
        "relativeToCurrentHubSpace",
      );
      expect(fileExplorer.props().initialFilePath).toBe("");

      // Navigate in SPACE
      fileExplorer.vm.$emit("navigate", "/space/new/path");
      await nextTick();

      // Switch to EMBEDDED tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "relative-to-embedded-data",
          path: "/initial/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().backendType).toBe("embedded");
      expect(fileExplorer.props().initialFilePath).toBe("");

      // Switch back to LOCAL tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "LOCAL",
          path: "/initial/path.txt",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().backendType).toBe("local");
      // Should still have the navigated path for LOCAL
      expect(fileExplorer.props().initialFilePath).toBe("/local/new/path");
    });
  });

  describe("path tracking during navigation", () => {
    it("tracks the current path when user navigates within a tab", async () => {
      const wrapper = mountSideDrawerContent({
        disabled: false,
        id: "test",
        modelValue: {
          fsCategory: "LOCAL",
          path: "",
          timeout: 1000,
        },
        uischema: {
          scope: "#/properties/test",
          options: {
            isLocal: true,
            spaceFSOptions: {
              mountId: "testSpace",
            },
          },
        },
      });

      await nextTick();

      let fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      expect(fileExplorer.props().initialFilePath).toBe("");

      // User navigates to a folder
      fileExplorer.vm.$emit("navigate", "/local/path");
      await nextTick();

      // Switch to SPACE tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "relative-to-current-hubspace",
          path: "",
          timeout: 1000,
        },
      });
      await nextTick();

      // Switch back to LOCAL tab
      await wrapper.setProps({
        modelValue: {
          fsCategory: "LOCAL",
          path: "",
          timeout: 1000,
        },
      });
      await nextTick();

      fileExplorer = wrapper.findComponent({ name: "DialogFileExplorer" });
      // LOCAL tab should remember the navigated path
      expect(fileExplorer.props().initialFilePath).toBe("/local/path");
    });
  });
});
