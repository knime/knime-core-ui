import { flushPromises, mount } from "@vue/test-utils";
import { useElementBounding } from "@vueuse/core";
import { Pane, type PaneProps, Splitpanes } from "splitpanes";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  defaultPortConfig,
  initConsoleEventHandler,
} from "@/__mocks__/scripting-service";
import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import InputOutputPane from "../InputOutputPane.vue";
import OutputConsole from "../OutputConsole.vue";
import ScriptingEditor from "../ScriptingEditor.vue";
import SettingsPage from "../SettingsPage.vue";
import { consoleHandler } from "@/consoleHandler";
import { nextTick, ref } from "vue";
import {
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  MIN_WIDTH_FOR_DISPLAYING_PANES,
} from "../utils/paneSizes";
import MainEditorPane from "../MainEditorPane.vue";
import ScriptingEditorBottomPane from "../ScriptingEditorBottomPane.vue";

import { DEFAULT_INITIAL_DATA } from "@/initial-data-service-browser-mock";
import { DEFAULT_INITIAL_SETTINGS } from "@/settings-service-browser-mock";
import { getScriptingService } from "@/scripting-service";

const mocks = vi.hoisted(() => {
  return {
    useElementBounding: vi.fn(),
  };
});

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    useElementBounding: mocks.useElementBounding,
    onKeyStroke: vi.fn(),
  };
});

vi.mock("xterm");

vi.mock("@/scripting-service");
vi.mock("@/editor");

vi.mock("@/settings-service", () => ({
  getSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettingsGetterForApply: vi.fn(() => Promise.resolve()),
  })),
}));

vi.mock("@/initial-data-service", () => ({
  getInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));

describe("ScriptingEditor", () => {
  beforeEach(() => {
    vi.mocked(useElementBounding).mockReturnValue({
      width: ref(MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE + 1),
    } as ReturnType<typeof useElementBounding>);
    vi.mocked(getScriptingService().sendToService).mockResolvedValue({});
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const doMount = (
    args: {
      props: Partial<InstanceType<typeof ScriptingEditor>["$props"]>;
      slots?: any;
    } = {
      props: {
        title: "myTitle",
        language: "someLanguage",
        rightPaneLayout: "fixed",
        fileName: "myFile.ts",
      },
    },
  ) => {
    const wrapper = mount(ScriptingEditor, {
      props: { title: "", language: "", fileName: "", ...args.props },
      slots: args.slots,
    });

    const findPane = (testId: string): PaneProps =>
      (wrapper.findComponent(`[data-testid="${testId}"]`) as any).props();
    const leftPane = findPane("leftPane");
    const mainPane = findPane("mainPane");
    const topPane = findPane("topPane");
    const bottomPane = findPane("bottomPane");
    const editorPane = findPane("editorPane");
    const rightPane = findPane("rightPane");

    const allSplitpanes = wrapper.findAllComponents(Splitpanes);
    const mainSplitpane = allSplitpanes.at(0);
    const horizontalSplitpane = allSplitpanes.at(1);
    const verticalSplitpane = allSplitpanes.at(2);
    if (
      typeof mainSplitpane === "undefined" ||
      typeof horizontalSplitpane === "undefined" ||
      typeof verticalSplitpane === "undefined"
    ) {
      throw new Error("could not find an expected splitpane");
    }
    return {
      wrapper,
      leftPane,
      mainPane,
      topPane,
      bottomPane,
      editorPane,
      rightPane,
      mainSplitpane,
      horizontalSplitpane,
      verticalSplitpane,
    };
  };

  describe("renders", () => {
    it("displays splitpanes", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(Splitpanes).exists()).toBeTruthy();
      expect(wrapper.findComponent(Pane).exists()).toBeTruthy();
    });

    it("mounts the main editor pane by default", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(MainEditorPane).exists()).toBeTruthy();
    });

    it("uses the provided title", () => {
      const { wrapper } = doMount();
      const heading = wrapper.find("div.title");
      expect(heading.element.textContent).toBe("myTitle");
    });

    it("display output console", () => {
      const { wrapper } = doMount();
      const outputConsole = wrapper.findComponent(OutputConsole);
      expect(outputConsole.exists()).toBeTruthy();
    });

    it("display input/output pane", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(InputOutputPane).exists()).toBeTruthy();
    });
  });

  describe("resizing and collapsing", () => {
    const resizeEvent = [{ size: 45 }, { size: 45 }];

    it("sets default initial sizes", () => {
      const { leftPane, mainPane, rightPane, bottomPane, editorPane, topPane } =
        doMount();
      expect(leftPane.size).toBe(20);
      expect(mainPane.size).toBe(80);
      expect(rightPane.size).toBe(25);
      expect(bottomPane.size).toBe(30);
      expect(editorPane.size).toBe(75);
      expect(topPane.size).toBe(70);
    });

    it("uses initial pane sizes when passed as props", () => {
      const { leftPane, mainPane, rightPane, bottomPane, editorPane, topPane } =
        doMount({
          props: {
            initialPaneSizes: {
              left: 10,
              right: 50,
              bottom: 40,
            },
          },
        });
      expect(leftPane.size).toBe(10);
      expect(mainPane.size).toBe(90);
      expect(rightPane.size).toBe(50);
      expect(editorPane.size).toBe(50);
      expect(bottomPane.size).toBe(40);
      expect(topPane.size).toBe(60);
    });

    it("set left/right and console pane sizes to zero when width is small", () => {
      vi.mocked(useElementBounding).mockReturnValue({
        width: ref(MIN_WIDTH_FOR_DISPLAYING_PANES - 1),
      } as ReturnType<typeof useElementBounding>);

      const { leftPane, rightPane, bottomPane } = doMount();
      expect(leftPane.size).toBe(0);
      expect(rightPane.size).toBe(0);
      expect(bottomPane.size).toBe(0);
    });

    it("set  and console pane sizes to zero when width is small", () => {
      vi.mocked(useElementBounding).mockReturnValue({
        width: ref(MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE - 1),
      } as ReturnType<typeof useElementBounding>);

      const { leftPane, rightPane, bottomPane } = doMount();
      expect(leftPane.size).toBe(0);
      expect(rightPane.size).not.toBe(0);
      expect(bottomPane.size).not.toBe(0);
    });

    describe("resizing", () => {
      it("resizes left pane", async () => {
        const { mainSplitpane, leftPane, mainPane } = doMount();
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
      });

      it("resizes right pane", async () => {
        const { horizontalSplitpane, bottomPane, topPane } = doMount();
        await horizontalSplitpane.vm.$emit("resize", resizeEvent);
        expect(bottomPane.size).toBe(45);
        expect(topPane.size).toBe(55);
      });

      it("resizes bottom pane", async () => {
        const { verticalSplitpane, rightPane, editorPane } = doMount();
        await verticalSplitpane.vm.$emit("resized", resizeEvent);
        expect(rightPane.size).toBe(45);
        expect(editorPane.size).toBe(55);
      });
    });

    describe("collapsing and uncollapsing", () => {
      it("collapses left pane", async () => {
        const { mainSplitpane, leftPane, mainPane } = doMount();
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(0);
        expect(mainPane.size).toBe(100);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(20);
        expect(mainPane.size).toBe(80);
      });

      it("collapses bottom pane", async () => {
        const { horizontalSplitpane, bottomPane, topPane } = doMount();
        await horizontalSplitpane.vm.$emit("splitter-click");
        expect(bottomPane.size).toBe(0);
        expect(topPane.size).toBe(100);
        await horizontalSplitpane.vm.$emit("splitter-click");
        expect(bottomPane.size).toBe(30);
        expect(topPane.size).toBe(70);
      });

      it("collapses right pane", async () => {
        const { verticalSplitpane, rightPane, editorPane } = doMount();
        await verticalSplitpane.vm.$emit("splitter-click", resizeEvent);
        expect(rightPane.size).toBe(0);
        expect(editorPane.size).toBe(100);
        await verticalSplitpane.vm.$emit("splitter-click", resizeEvent);
        expect(rightPane.size).toBe(25);
        expect(editorPane.size).toBe(75);
      });

      it("rotates splitter icon for left pane", async () => {
        const { mainSplitpane } = doMount();
        expect(mainSplitpane.classes()).toContain("left-facing-splitter");
        expect(mainSplitpane.classes()).not.toContain("right-facing-splitter");
        await mainSplitpane.vm.$emit("splitter-click");
        expect(mainSplitpane.classes()).not.toContain("left-facing-splitter");
        expect(mainSplitpane.classes()).toContain("right-facing-splitter");
      });

      it("rotates splitter icon for bottom pane", async () => {
        const { horizontalSplitpane } = doMount();
        expect(horizontalSplitpane.classes()).not.toContain(
          "up-facing-splitter",
        );
        expect(horizontalSplitpane.classes()).toContain("down-facing-splitter");
        await horizontalSplitpane.vm.$emit("splitter-click");
        expect(horizontalSplitpane.classes()).toContain("up-facing-splitter");
        expect(horizontalSplitpane.classes()).not.toContain(
          "down-facing-splitter",
        );
      });

      it("rotates splitter icon for right pane", async () => {
        const { verticalSplitpane } = doMount();
        expect(verticalSplitpane.classes()).not.toContain(
          "left-facing-splitter",
        );
        expect(verticalSplitpane.classes()).toContain("right-facing-splitter");
        await verticalSplitpane.vm.$emit("splitter-click");
        expect(verticalSplitpane.classes()).toContain("left-facing-splitter");
        expect(verticalSplitpane.classes()).not.toContain(
          "right-facing-splitter",
        );
      });

      it("uncollapses pane to correct position when collapsing after resize", async () => {
        const { mainSplitpane, leftPane, mainPane } = doMount();
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
        await mainSplitpane.vm.$emit("resized", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(0);
        expect(mainPane.size).toBe(100);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
      });

      it("uncollapses to drag-start position when pane was resized to 0 size", async () => {
        const { mainSplitpane, leftPane, mainPane } = doMount();
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
        await mainSplitpane.vm.$emit("resized", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
        await mainSplitpane.vm.$emit("resize", [{ size: 0 }]);
        expect(leftPane.size).toBe(0);
        expect(mainPane.size).toBe(100);
        await mainSplitpane.vm.$emit("resized", [{ size: 0 }]);
        expect(leftPane.size).toBe(0);
        expect(mainPane.size).toBe(100);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
      });
    });

    describe("right pane resizing on change in left pane", () => {
      const resizeEvent = [{ size: 60 }];

      it("adjusts right pane size if left pane is resized and right pane uses fixed layout", async () => {
        const { mainSplitpane, rightPane } = doMount();
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(rightPane.size).toBe(50);
      });

      it("adjusts right pane size if left pane is collapsed and right pane uses fixed layout", async () => {
        const { mainSplitpane, rightPane } = doMount();
        await mainSplitpane.vm.$emit("splitter-click");
        expect(rightPane.size).toBe(20);
      });

      it("does not change right pane size if left pane is resized and right pane uses relative layout", async () => {
        const { mainSplitpane, rightPane } = doMount({
          props: { rightPaneLayout: "relative" },
        });
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(rightPane.size).toBe(25);
      });

      it("does not change right pane size if left pane is collapsed and right pane uses relative layout", async () => {
        const { mainSplitpane, rightPane } = doMount({
          props: { rightPaneLayout: "relative" },
        });
        await mainSplitpane.vm.$emit("splitter-click");
        expect(rightPane.size).toBe(25);
      });
    });
  });

  describe("code editor bottom control bar", () => {
    it("is visible by default", () => {
      const { wrapper } = doMount();
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeTruthy();
    });

    it("are visible if enabled", () => {
      const { wrapper } = doMount({
        props: { showControlBar: true },
      });
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeTruthy();
    });

    it("are not visible if disabled", () => {
      const { wrapper } = doMount({
        props: { showControlBar: false },
      });
      expect(wrapper.findComponent(CodeEditorControlBar).exists()).toBeFalsy();
    });

    it("displays slotted content if enabled", () => {
      const { wrapper } = doMount({
        props: {},
        slots: {
          "code-editor-controls":
            "<button class='test-class'>TestButton</button>",
        },
      });
      expect(
        wrapper
          .findComponent(CodeEditorControlBar)
          .find(".test-class")
          .exists(),
      ).toBeTruthy();
    });
  });

  it("passes slotted content to bottom pane", () => {
    const { wrapper } = doMount({
      props: {},
      slots: {
        "console-status": "<div class='test-class'>Test</div>",
      },
    });
    expect(
      wrapper
        .findComponent(ScriptingEditorBottomPane)
        .find(".test-class")
        .exists(),
    ).toBeTruthy();
  });

  it("sets console handler store on console-created", async () => {
    // setup
    const { wrapper } = doMount();
    const outputConsole = wrapper.findComponent(OutputConsole);

    await flushPromises();
    expect(outputConsole.emitted()).toHaveProperty("console-created");

    // @ts-ignore
    const handler = outputConsole.emitted()["console-created"][0][0];
    expect(consoleHandler).toBe(handler);
  });

  it("registers console event handler on console-created", async () => {
    // setup
    const { wrapper } = doMount();
    const outputConsole = wrapper.findComponent(OutputConsole);

    await flushPromises();
    expect(outputConsole.emitted()).toHaveProperty("console-created");

    // @ts-ignore
    expect(initConsoleEventHandler).toHaveBeenCalledOnce();
  });

  it("shows settings page", async () => {
    const { wrapper } = doMount();
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    expect(wrapper.findComponent(SettingsPage).exists()).toBeTruthy();
  });

  it("closes settings page", async () => {
    const { wrapper } = doMount();
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    const settingsPage = wrapper.findComponent(SettingsPage);
    expect(settingsPage.exists()).toBeTruthy();
    settingsPage.vm.$emit("close-settings-page");
    await wrapper.vm.$nextTick();
    expect(settingsPage.exists()).toBeFalsy();
  });

  it("passes slotted content through to settings page", async () => {
    const { wrapper } = doMount({
      props: {
        title: "myTitle",
        language: "someLanguage",
        rightPaneLayout: "fixed",
        fileName: "myFile.ts",
      },
      slots: {
        "settings-title": "<div class='settings-title'>Settings title</div>",
        "settings-content":
          "<div class='settings-content'>Settings content</div>",
      },
    });
    (wrapper.vm as any).showSettingsPage = true;
    await wrapper.vm.$nextTick();
    const settingsPage = wrapper.findComponent(SettingsPage);
    expect(settingsPage.find(".settings-title").exists()).toBeTruthy();
    expect(settingsPage.find(".settings-content").exists()).toBeTruthy();
  });

  it("sets drop event on code editor", async () => {
    const { wrapper } = doMount();
    const inOutPane = wrapper.findComponent(InputOutputPane);
    expect(inOutPane.exists()).toBeTruthy();

    const dropEventHandlerMock = vi.fn();
    const dropEventMock = { drop: "event" };
    inOutPane.vm.$emit("drop-event-handler-created", dropEventHandlerMock);
    expect((wrapper.vm as any).dropEventHandler).toStrictEqual(
      dropEventHandlerMock,
    );
    await nextTick();

    const editorComponent = wrapper.findComponent(MainEditorPane);
    const editorTextField = editorComponent.find(".code-editor");

    expect(editorTextField.exists()).toBeTruthy();
    expect(editorComponent.props().dropEventHandler).toBe(dropEventHandlerMock);

    await editorTextField.trigger("drop", dropEventMock);

    expect(dropEventHandlerMock).toHaveBeenCalledWith(
      expect.objectContaining(dropEventMock),
    );
  });

  it("displays anything we pass through via the 'editor' slot", () => {
    const { wrapper } = doMount({
      props: {},
      slots: {
        editor: "<p id='testeditorslotinjection'>Test</p>",
      },
    });

    expect(wrapper.find("#testeditorslotinjection").exists()).toBeTruthy();
  });

  it("shows input table tabs for port views", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    const makeNodePortId = (nodeId: string, portIdx: number) =>
      `${nodeId}-${portIdx}`;
    const tabElements = wrapper.findAll(".tab-bar input");

    for (const inputPort of defaultPortConfig.inputPorts) {
      const expectedElement = tabElements.find(
        (tab) =>
          tab.attributes("value") ===
          makeNodePortId(inputPort.nodeId!, inputPort.portIdx),
      );

      expect(expectedElement).toBeDefined();
      expect(expectedElement!.isVisible()).toBeTruthy();
    }
  });
});
