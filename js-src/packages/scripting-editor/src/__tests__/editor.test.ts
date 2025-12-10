import { beforeEach, describe, expect, it, vi } from "vitest";
import { type Ref, defineComponent, toRaw } from "vue";
import { mount } from "@vue/test-utils";
import * as monaco from "monaco-editor";

import {
  type UseCodeEditorParams,
  type UseDiffEditorParams,
  useCodeEditor,
  useDiffEditor,
  useMainCodeEditor,
  useMainCodeEditorStore,
} from "../editor";

describe("editor", () => {
  const testComponent = <T>(
    composable: (params: any) => T,
    provideContainer = true,
  ) =>
    defineComponent({
      props: {
        params: {
          type: Object,
          required: true,
        },
      },
      setup(props) {
        return {
          returnVal: composable({
            container: {
              ...(provideContainer
                ? { value: document.createElement("div") }
                : {}),
            },
            ...toRaw(props.params),
          }),
        };
      },
      template: "<div></div>",
    });

  // @ts-expect-error createModel is mocked and does not need the arguments
  const createEditorModel = () => monaco.editor.createModel();

  const expectModelProvidesText = (
    editorModel: monaco.editor.ITextModel,
    text: Ref<string>,
  ) => {
    expect(editorModel.onDidChangeContent).toHaveBeenCalledOnce();
    expect(text.value).toBe("myInitialScript");
    vi.mocked(editorModel.getValue).mockReturnValueOnce("bar");
    vi.mocked(editorModel.onDidChangeContent).mock.lastCall?.[0]({} as any);
    expect(text.value).toBe("bar");
  };

  const expectTextEditsModel = (
    editorModel: monaco.editor.ITextModel,
    text: Ref<string>,
  ) => {
    expect(text.value).toBe("myInitialScript");
    text.value = "foo";
    expect(editorModel.pushStackElement).toHaveBeenCalledOnce();
    expect(editorModel.pushEditOperations).toHaveBeenCalledOnce();
    expect(editorModel.pushEditOperations).toHaveBeenCalledWith(
      [],
      [{ range: "myRange", text: "foo" }],
      expect.any(Function),
    );
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe("useCodeEditor", () => {
    const mountTestComponent = (
      params: Omit<UseCodeEditorParams, "container" | "extraEditorOptions"> = {
        language: "javascript",
        fileName: "test.js",
      },
    ) => {
      const wrapper = mount(testComponent(useCodeEditor), {
        props: { params },
      });
      const state = wrapper.vm.returnVal;
      return { wrapper, state };
    };

    it("creates a new editor model", () => {
      const { state } = mountTestComponent();
      expect(monaco.editor.createModel).toHaveBeenCalledOnce();
      expect(monaco.editor.createModel).toHaveBeenCalledWith(
        "",
        "javascript",
        "inmemory://model/test.js",
      );
      expect(state.editorModel).toBeDefined();
      expect(state.editorModel).toBe(
        vi.mocked(monaco.editor.createModel).mock.results[0].value,
      );
    });

    it("re-uses an existing editor model with same fileName", () => {
      const existingModel = {
        getValue: vi.fn(() => ""),
        onDidChangeContent: vi.fn(),
      } as any as monaco.editor.ITextModel;
      vi.mocked(monaco.editor.getModel).mockReturnValue(existingModel);
      const { state } = mountTestComponent();
      expect(monaco.editor.getModel).toHaveBeenCalledOnce();
      expect(monaco.editor.getModel).toHaveBeenCalledWith(
        "inmemory://model/test.js",
      );
      expect(monaco.editor.createModel).not.toHaveBeenCalled();
      expect(state.editorModel).toBeDefined();
      expect(state.editorModel).toBe(existingModel);
    });

    it("re-uses an existing editor model if given", () => {
      const existingModel = {
        getValue: vi.fn(() => ""),
        onDidChangeContent: vi.fn(),
      } as any as monaco.editor.ITextModel;
      const { state } = mountTestComponent({ editorModel: existingModel });
      expect(monaco.editor.getModel).not.toHaveBeenCalled();
      expect(monaco.editor.createModel).not.toHaveBeenCalled();
      expect(state.editorModel).toBeDefined();
      expect(state.editorModel).toBe(existingModel);
    });

    it("provides editorModel value as text", () => {
      const { state } = mountTestComponent();
      expectModelProvidesText(state.editorModel, state.text);
    });

    it("edits editorModel value on text change", () => {
      const { state } = mountTestComponent();
      expectTextEditsModel(state.editorModel, state.text);
    });

    it("sets editorModel value on setInitialText", () => {
      const { state } = mountTestComponent();
      state.setInitialText("bar");
      expect(state.editorModel.setValue).toHaveBeenCalledOnce();
      expect(state.editorModel.setValue).toHaveBeenCalledWith("bar");
    });

    it("throws error onMounted if no element is provided", () => {
      const editor = testComponent(useCodeEditor, false);
      expect(() => mount(editor, { props: { params: {} } })).toThrowError(
        "Could not create code editor because no element was provided.",
      );
    });

    it("creates editor onMounted", () => {
      const { state } = mountTestComponent();
      expect(monaco.editor.create).toHaveBeenCalledOnce();
      expect(monaco.editor.create).toHaveBeenCalledWith(
        expect.any(HTMLDivElement),
        expect.objectContaining({ model: state.editorModel }),
      );
    });

    it("updates selection on cursor selection change", () => {
      const { state } = mountTestComponent();
      expect(
        state.editor.value?.onDidChangeCursorSelection,
      ).toHaveBeenCalledOnce();
      expect(state.selection.value).toBe("");
      expect(state.selectedLines.value).toBe("");

      const selectionEvent = {
        selection: {
          startLineNumber: 123,
          endLineNumber: 127,
        },
      } as any;
      vi.mocked(
        state.editor.value?.onDidChangeCursorSelection,
      )?.mock.lastCall?.[0](selectionEvent);

      // Call for getting the selection
      expect(state.editorModel.getValueInRange).toHaveBeenCalledWith(
        selectionEvent.selection,
      );
      expect(state.selection.value).toBe("mySelectedRange");

      // Call for getting the selected lines
      expect(state.editorModel.getValueInRange).toHaveBeenCalledWith({
        startLineNumber: 123,
        startColumn: 0,
        endLineNumber: 127,
        endColumn: 100,
      });
      expect(state.selectedLines.value).toBe("mySelectedRange");
    });

    it("disposes editor onUnmounted", () => {
      const { wrapper, state } = mountTestComponent();
      expect(state.editor.value?.dispose).not.toHaveBeenCalled();
      wrapper.unmount();
      expect(state.editor.value?.dispose).toHaveBeenCalledOnce();
      expect(state.editorModel.dispose).not.toHaveBeenCalledOnce();
    });

    it("disposes editorModel onUnmounted if not attached to any editor", () => {
      const { wrapper, state } = mountTestComponent();
      expect(state.editorModel.dispose).not.toHaveBeenCalled();
      vi.mocked(state.editorModel.isAttachedToEditor).mockReturnValue(false);
      wrapper.unmount();
      expect(state.editorModel.dispose).toHaveBeenCalledOnce();
    });
  });

  describe("useDiffEditor", () => {
    const mountTestComponent = (
      params: Omit<UseDiffEditorParams, "container"> = {
        originalModel: null as any,
        modifiedFileName: "modified.js",
      },
    ) => {
      params.originalModel = params.originalModel || createEditorModel();
      const wrapper = mount(testComponent(useDiffEditor), {
        props: { params },
      });
      const state = wrapper.vm.returnVal;
      return { wrapper, state };
    };

    it("creates a new modified editor model", () => {
      const originalModel = createEditorModel();
      const { state } = mountTestComponent({
        originalModel,
        modifiedFileName: "modified.js",
      });
      expect(monaco.editor.createModel).toHaveBeenCalledWith(
        "",
        "myLanguageId",
        "inmemory://model/modified.js",
      );
      expect(state.editorModel.original).toBe(originalModel);
      expect(state.editorModel.modified).toBe(
        vi.mocked(monaco.editor.createModel).mock.results[1].value,
      );
    });

    it("provides editorModel value as modifiedText", () => {
      const { state } = mountTestComponent();
      expectModelProvidesText(state.editorModel.modified, state.modifiedText);
    });

    it("edits editorModel value on modifiedText change", () => {
      const { state } = mountTestComponent();
      expectTextEditsModel(state.editorModel.modified, state.modifiedText);
    });

    it("sets editorModel value on setInitialModifiedText", () => {
      const { state } = mountTestComponent();
      state.setInitialModifiedText("bar");
      expect(state.editorModel.modified.setValue).toHaveBeenCalledOnce();
      expect(state.editorModel.modified.setValue).toHaveBeenCalledWith("bar");
    });

    it("throws error onMounted if no element is provided", () => {
      const editor = testComponent(useCodeEditor, false);
      expect(() => mount(editor, { props: { params: {} } })).toThrowError(
        "Could not create code editor because no element was provided.",
      );
    });

    it("disposes editor onUnmounted", () => {
      const { wrapper, state } = mountTestComponent();
      expect(state.editor.value?.dispose).not.toHaveBeenCalled();
      wrapper.unmount();
      expect(state.editor.value?.dispose).toHaveBeenCalledOnce();
      expect(state.editorModel.modified.dispose).not.toHaveBeenCalledOnce();
      expect(state.editorModel.original.dispose).not.toHaveBeenCalledOnce();
    });

    it("disposes modified editorModel onUnmounted if not attached to any editor", () => {
      const { wrapper, state } = mountTestComponent();
      expect(state.editorModel.modified.dispose).not.toHaveBeenCalled();
      vi.mocked(state.editorModel.modified.isAttachedToEditor).mockReturnValue(
        false,
      );
      wrapper.unmount();
      expect(state.editorModel.modified.dispose).toHaveBeenCalledOnce();
    });

    it("disposes original editorModel onUnmounted if not attached to any editor", () => {
      const { wrapper, state } = mountTestComponent();
      expect(state.editorModel.original.dispose).not.toHaveBeenCalled();
      vi.mocked(state.editorModel.original.isAttachedToEditor).mockReturnValue(
        false,
      );
      wrapper.unmount();
      expect(state.editorModel.original.dispose).toHaveBeenCalledOnce();
    });
  });

  describe("useMainCodeEditor", () => {
    const mountTestComponent = () => {
      const wrapper = mount(testComponent(useMainCodeEditor), {
        props: { params: { language: "javascript", fileName: "test.js" } },
      });
      const state = wrapper.vm.returnVal;
      return { wrapper, state };
    };

    it("stores editor state", () => {
      const { state } = mountTestComponent();
      expect(useMainCodeEditorStore().value).toBe(state);
    });
  });
});
