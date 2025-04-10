import { beforeEach, describe, expect, it, vi } from "vitest";
import { type Ref, nextTick, ref } from "vue";
import { shallowMount } from "@vue/test-utils";

import DialogFileExplorer from "../../DialogFileExplorer.vue";
import { applyButtonInjectionKey } from "../../settingsSubPanel";
import FileExplorerTab, {
  type FileExplorerTabProps,
} from "../FileExplorerTab.vue";

describe("FileExplorerTab.vue", () => {
  let props: FileExplorerTabProps,
    applyDisabled: Ref<boolean>,
    onApply: Ref<undefined | (() => Promise<void>)>;

  beforeEach(() => {
    applyDisabled = ref(false);
    onApply = ref(undefined);
    props = {
      backendType: "relativeToCurrentHubSpace",
      selectionMode: "FILE",
    };
  });

  const mountFileExplorerTab = () => {
    return shallowMount(FileExplorerTab, {
      props,
      global: {
        provide: {
          [applyButtonInjectionKey as symbol]: {
            element: ref(null),
            disabled: applyDisabled,
            text: ref("initialText"),
            hidden: ref(false),
            onApply,
          },
        },
      },
    });
  };

  it("renders", () => {
    const wrapper = mountFileExplorerTab();

    const dialogFileExplorer = wrapper.findComponent(DialogFileExplorer);
    expect(dialogFileExplorer.exists()).toBeTruthy();
    expect(dialogFileExplorer.props()).toMatchObject({
      ...props,
      initialFilePath: "",
      isWriter: false,
      filteredExtensions: [],
      appendedExtension: null,
    });
  });

  it("disables applying if no file is selected, enables when file is selected", async () => {
    const wrapper = mountFileExplorerTab();
    expect(applyDisabled.value).toBeTruthy();

    wrapper.findComponent(DialogFileExplorer).vm.$emit("update:selectedItem", {
      name: "myFile",
      selectionType: "FILE",
    });
    await nextTick();

    expect(applyDisabled.value).toBeFalsy();

    wrapper
      .findComponent(DialogFileExplorer)
      .vm.$emit("update:selectedItem", null);
    await nextTick();

    expect(applyDisabled.value).toBeTruthy();
  });

  it("opens file on apply", async () => {
    const wrapper = mountFileExplorerTab();
    const file = "myFile";
    const chooseSelectedItem = vi.fn().mockResolvedValue(file);
    wrapper.findComponent(DialogFileExplorer).vm.chooseSelectedItem =
      chooseSelectedItem;
    await onApply.value!();
    expect(chooseSelectedItem).toHaveBeenCalled();
  });

  it("passes through emitted chooseFile events", () => {
    const wrapper = mountFileExplorerTab();
    const dialogFileExplorer = wrapper.findComponent(DialogFileExplorer);
    const path = "myPath";

    dialogFileExplorer.vm.$emit("chooseItem", path);

    expect(wrapper.emitted("chooseItem")).toStrictEqual([[path]]);
  });
});
