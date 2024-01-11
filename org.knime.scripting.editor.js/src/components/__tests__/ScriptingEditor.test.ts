import { flushPromises, mount } from "@vue/test-utils";
import { onKeyStroke } from "@vueuse/core";
import { Pane, Splitpanes, type PaneProps } from "splitpanes";
import { afterEach, describe, expect, it, vi } from "vitest";

import { initConsoleEventHandler } from "@/__mocks__/scripting-service";
import CodeEditorControlBar from "../CodeEditorControlBar.vue";
import FooterBar from "../FooterBar.vue";
import InputOutputPane from "../InputOutputPane.vue";
import OutputConsole from "../OutputConsole.vue";
import ScriptingEditor from "../ScriptingEditor.vue";
import SettingsPage from "../SettingsPage.vue";
import { useMainCodeEditor } from "@/editor";
import { consoleHandlerStore } from "@/consoleHandler";

vi.mock("xterm");
vi.mock("@vueuse/core");

vi.mock("@/scripting-service");
vi.mock("@/editor");

describe("ScriptingEditor", () => {
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
    const { leftPane, mainPane, topPane, bottomPane, editorPane, rightPane } =
      wrapper.vm.$refs as Record<string, PaneProps>;
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

    it("displays code editor and passes props", () => {
      doMount();
      expect(useMainCodeEditor).toHaveBeenCalledWith({
        container: expect.anything(),
        language: "someLanguage",
        fileName: "myFile.ts",
      });
      expect(
        vi.mocked(useMainCodeEditor).mock.calls[0][0].container.value,
      ).toBeDefined();
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

  it("should register onKeyStroke handler", () => {
    doMount();
    expect(onKeyStroke).toHaveBeenCalledWith("z", expect.anything());
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

  it("passes slotted content to output console", () => {
    const { wrapper } = doMount({
      props: {},
      slots: {
        "console-status": "<div class='test-class'>Test</div>",
      },
    });
    expect(
      wrapper.findComponent(OutputConsole).find(".test-class").exists(),
    ).toBeTruthy();
  });

  it("saves settings", async () => {
    const { wrapper } = doMount();
    await flushPromises();
    const comp = wrapper.findComponent(FooterBar);
    comp.vm.$emit("scriptingEditorOkayed");
    expect(wrapper.emitted("save-settings")).toBeDefined();
    expect(wrapper.emitted("save-settings")?.length).toBe(1);
    // @ts-ignore
    expect(wrapper.emitted("save-settings")[0][0]).toStrictEqual({
      script: "myInitialScript",
    });
  });

  it("sets console handler store on console-created", async () => {
    // setup
    const { wrapper } = doMount();
    const outputConsole = wrapper.findComponent(OutputConsole);

    await flushPromises();
    expect(outputConsole.emitted()).toHaveProperty("console-created");

    // @ts-ignore
    const handler = outputConsole.emitted()["console-created"][0][0];
    expect(consoleHandlerStore.value).toBe(handler);
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

  it("sets drop event on code editor", () => {
    const { wrapper } = doMount();
    const inOutPane = wrapper.findComponent(InputOutputPane);
    expect(inOutPane.exists()).toBeTruthy();
    const dropEventHandlerMock = vi.fn();
    const dropEventMock = { drop: "event" };
    inOutPane.vm.$emit("drop-event-handler-created", dropEventHandlerMock);
    expect((wrapper.vm as any).dropEventHandler).toStrictEqual(
      dropEventHandlerMock,
    );
    wrapper.find(".code-editor").trigger("drop", dropEventMock);
    expect(dropEventHandlerMock).toHaveBeenCalledWith(
      expect.objectContaining(dropEventMock),
    );
  });
});
