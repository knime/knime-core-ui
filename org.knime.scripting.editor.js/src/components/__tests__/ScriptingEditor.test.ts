import { describe, expect, it, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import ScriptingEditor from "../ScriptingEditor.vue";
import { Pane, Splitpanes } from "splitpanes";
import CodeEditor from "../CodeEditor.vue";
import { RightPaneLayout } from "../../types/scriptingEditor";

vi.mock("monaco-editor", () => {
  return {
    editor: {
      createModel: vi.fn(() => "myModel"),
      create: vi.fn((element: HTMLElement) => {
        element.innerHTML = "SCRIPTING EDITOR MOCK";
        return "myEditor";
      }),
    },
    Uri: {
      parse: vi.fn((path: string) => path),
    },
  };
});

describe("ScriptingEditor", () => {
  let wrapper;

  const props = {
    language: "someLanguage",
    initialScript: "someInitialScript",
    RightPaneLayout: RightPaneLayout.FIXED,
  };

  describe("renders", () => {
    beforeEach(() => {
      wrapper = mount(ScriptingEditor, {
        props,
      });
    });

    it("displays splitpanes", () => {
      expect(wrapper.findComponent(Splitpanes).exists()).toBeTruthy();
      expect(wrapper.findComponent(Pane).exists()).toBeTruthy();
    });

    it("displays code editor and passes props", () => {
      expect(wrapper.findComponent(CodeEditor).exists()).toBeTruthy();
      const comp = wrapper.findComponent(CodeEditor);
      expect(comp.vm.initialScript).toBe("someInitialScript");
      expect(comp.vm.language).toBe("someLanguage");
    });
  });

  describe("resizing and collapsing", () => {
    let wrapper,
      leftPane,
      mainPane,
      topPane,
      bottomPane,
      editorPane,
      rightPane,
      mainSplitpane,
      verticalSplitpane,
      horizontalSplitpane;

    const resizeEvent = [{ size: 45 }, { size: 45 }];

    beforeEach(() => {
      wrapper = mount(ScriptingEditor, {
        props,
      });
      ({ leftPane, mainPane, topPane, bottomPane, editorPane, rightPane } =
        wrapper.vm.$refs);
      mainSplitpane = wrapper.findAllComponents(Splitpanes).at(0);
      horizontalSplitpane = wrapper.findAllComponents(Splitpanes).at(1);
      verticalSplitpane = wrapper.findAllComponents(Splitpanes).at(2);
    });

    it("sets initial sizes", () => {
      expect(leftPane.size).toBe(20);
      expect(mainPane.size).toBe(80);
      expect(rightPane.size).toBe(25);
      expect(bottomPane.size).toBe(30);
      expect(editorPane.size).toBe(75);
      expect(topPane.size).toBe(70);
    });

    describe("resizing", () => {
      it("resizes left pane", async () => {
        await mainSplitpane.vm.$emit("resized", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
      });

      it("resizes right pane", async () => {
        await horizontalSplitpane.vm.$emit("resized", resizeEvent);
        expect(bottomPane.size).toBe(45);
        expect(topPane.size).toBe(55);
      });

      it("resizes bottom pane", async () => {
        await verticalSplitpane.vm.$emit("resized", resizeEvent);
        expect(rightPane.size).toBe(45);
        expect(editorPane.size).toBe(55);
      });
    });

    describe("collapsing and uncollapsing", () => {
      it("collapses left pane", async () => {
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(0);
        expect(mainPane.size).toBe(100);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(leftPane.size).toBe(20);
        expect(mainPane.size).toBe(80);
      });

      it("collapses right pane", async () => {
        await horizontalSplitpane.vm.$emit("splitter-click");
        expect(bottomPane.size).toBe(0);
        expect(topPane.size).toBe(100);
        await horizontalSplitpane.vm.$emit("splitter-click");
        expect(bottomPane.size).toBe(30);
        expect(topPane.size).toBe(70);
      });

      it("collapses bottom pane", async () => {
        await verticalSplitpane.vm.$emit("splitter-click", resizeEvent);
        expect(rightPane.size).toBe(0);
        expect(editorPane.size).toBe(100);
        await verticalSplitpane.vm.$emit("splitter-click", resizeEvent);
        expect(rightPane.size).toBe(25);
        expect(editorPane.size).toBe(75);
      });

      it("uncollapses pane to correct position when collapsing after resize", async () => {
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
        await mainSplitpane.vm.$emit("resized", resizeEvent);
        expect(leftPane.size).toBe(45);
        expect(mainPane.size).toBe(55);
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
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(rightPane.size).toBe(50);
      });

      it("adjusts right pane size if left pane is collapsed and right pane uses fixed layout", async () => {
        await mainSplitpane.vm.$emit("splitter-click");
        expect(rightPane.size).toBe(20);
      });

      it("does not change right pane size if left pane is resized and right pane uses relative layout", async () => {
        wrapper = mount(ScriptingEditor, {
          props: { rightPaneLayout: RightPaneLayout.RELATIVE },
        });
        mainSplitpane = wrapper.findAllComponents(Splitpanes).at(0);
        await mainSplitpane.vm.$emit("resize", resizeEvent);
        expect(rightPane.size).toBe(25);
      });

      it("does not change right pane size if left pane is collapsed and right pane uses relative layout", async () => {
        wrapper = mount(ScriptingEditor, {
          props: { rightPaneLayout: RightPaneLayout.RELATIVE },
        });
        mainSplitpane = wrapper.findAllComponents(Splitpanes).at(0);
        await mainSplitpane.vm.$emit("splitter-click");
        expect(rightPane.size).toBe(25);
      });
    });
  });
});
