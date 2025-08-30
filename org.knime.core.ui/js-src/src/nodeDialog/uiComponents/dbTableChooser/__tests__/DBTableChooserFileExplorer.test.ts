import { describe, expect, it, vi } from "vitest";
import { nextTick, ref } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { Breadcrumb, FileExplorer } from "@knime/components";

import { GO_INTO_FOLDER_INJECTION_KEY } from "../../fileChooser/settingsSubPanel/SettingsSubPanelForFileChooser.vue";
import {
  applyButtonInjectionKey,
  createOrGetInjectionKey,
} from "../../fileChooser/settingsSubPanel/useApplyButton";
import DBTableChooserFileExplorer from "../DBTableChooserFileExplorer.vue";

describe("DBTableChooserFileExplorer", () => {
  const onApply = ref(vi.fn());
  const dataServiceSpyCreator = (useCatalogues: boolean) =>
    vi.fn(async ({ method, options }: { method: string; options: any[] }) => {
      const backendMock = (
        await import("../../../mocks/dbTableChooserMock")
      ).makeTableChooserBackendMock(useCatalogues);
      if (method === "dbTableChooser.listItems") {
        return backendMock.listItems(options[0], options[1]);
      } else if (method === "dbTableChooser.itemType") {
        return backendMock.itemType(options[0]);
      }
      throw new Error(`Unknown method: ${method}`);
    });

  const doMount = async (useCatalogues: boolean, initialPath: string = "") => {
    const dataServiceSpyInstance = dataServiceSpyCreator(useCatalogues);

    const context = {
      global: {
        provide: {
          getData: dataServiceSpyInstance,
          [applyButtonInjectionKey as symbol]: {
            element: null,
            disabled: ref(false),
            shown: ref(true),
            text: ref("initialText"),
            onApply,
          },
          [createOrGetInjectionKey(GO_INTO_FOLDER_INJECTION_KEY) as symbol]: {
            shown: ref(false),
            element: null,
            disabled: ref(false),
            onApply: ref(undefined),
            text: ref("Something"),
          },
        },
      },
      props: {
        initialPathParts: initialPath.split("/"),
        initialTable: "helloWorld",
      },
    };

    const wrapper = mount(DBTableChooserFileExplorer, context);

    // I think we have a lot of promises that have to clear before this component
    // actually renders all its parts. Without this giant list of flushPromises,
    // the FileExplorer component doesn't seem to show up correctly.
    await nextTick();
    await flushPromises();

    await new Promise((resolve) => setTimeout(resolve, 500));

    return {
      wrapper,
      dataServiceSpy: dataServiceSpyInstance,
    };
  };

  it("renders", async () => {
    const { wrapper } = await doMount(true);
    expect(
      wrapper.findComponent(DBTableChooserFileExplorer).exists(),
    ).toBeTruthy();
  });

  it("calls listItems on mount with the correct arguments", async () => {
    const { dataServiceSpy } = await doMount(true, "something/somethingElse");
    expect(dataServiceSpy).toHaveBeenCalledWith({
      method: "dbTableChooser.listItems",
      options: [["something", "somethingElse"], "helloWorld"],
    });
  });

  it("shows breadcrumbs", async () => {
    const { wrapper } = await doMount(true, "testCatalog/testSchema");
    expect(
      wrapper
        .findComponent(Breadcrumb)
        .props()
        .items?.map((item) => item.text),
    ).toStrictEqual(["", "testCatalog", "testSchema"]);
  });

  it("opens a directory (aka schema or catalogue) via FileExplorer event", async () => {
    const { wrapper, dataServiceSpy } = await doMount(true, "testCatalog");

    wrapper
      .findComponent(FileExplorer)
      .vm.$emit("changeDirectory", "testCatalog/testSchema");

    await flushPromises();
    await nextTick();

    expect(dataServiceSpy).toHaveBeenCalledWith({
      method: "dbTableChooser.listItems",
      options: [["testCatalog", "testSchema"], null],
    });
  });

  it("opens a directory (aka schema or catalog) via breadcrumb click", async () => {
    const { wrapper, dataServiceSpy } = await doMount(
      true,
      "testCatalog/testSchema",
    );

    wrapper
      .findComponent(Breadcrumb)
      .vm.$emit("click-item", { text: "testCatalog", path: "testCatalog" });

    await nextTick();

    expect(dataServiceSpy).toHaveBeenCalledWith({
      method: "dbTableChooser.listItems",
      options: [["testCatalog"], null],
    });
  });

  it("opens a file (aka table) via FileExplorer event", async () => {
    const { wrapper } = await doMount(true, "testCatalog/testSchema");

    wrapper.findComponent(FileExplorer).vm.$emit("openFile", {
      name: "testTable",
      meta: {
        type: "TABLE",
        tableMetadata: {
          tableType: "TABLE",
          containingSchema: "mySchema",
          containingCatalogue: "myCatalog",
        },
      },
    });

    const emitted = wrapper.emitted("tableSelected");

    expect(emitted).toBeDefined();
    expect(emitted![0][0]).toStrictEqual([
      "myCatalog",
      "mySchema",
      "testTable",
    ]);
  });
});
