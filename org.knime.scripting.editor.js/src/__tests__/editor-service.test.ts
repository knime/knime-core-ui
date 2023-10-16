import { EditorService } from "@/editor-service";
import { editor } from "__mocks__/monaco-editor";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("monaco-editor");

const editorMock = editor.create({ innerHTML: "" } as any);
const editorModelMock = editor.createModel();

describe("editor-service", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("initializes editor service", () => {
    const editorService = new EditorService();
    editorService.initEditorService({
      editor: editorMock as any,
      editorModel: editorModelMock as any,
    });
    expect(editorService.editor).toStrictEqual(editorMock);
    expect(editorService.editorModel).toStrictEqual(editorModelMock);
  });

  it("getScript returns script", () => {
    const editorService = new EditorService();
    editorService.initEditorService({
      editor: editorMock as any,
      editorModel: editorModelMock as any,
    });
    expect(editorService.getScript()).toBe("myInitialScript");
  });

  it("getScript returns null if editor is not initialized", () => {
    const editorService = new EditorService();
    expect(editorService.getScript()).toBeNull();
    editorService.editor = editorMock as any;
    expect(editorService.getScript()).toBeNull();
    editorService.editor = undefined;
    editorService.editorModel = editorModelMock as any;
    expect(editorService.getScript()).toBeNull();
  });

  it("getSelectedLines returns selected lines", () => {
    const editorService = new EditorService();
    editorService.initEditorService({
      editor: editorMock as any,
      editorModel: editorModelMock as any,
    });
    expect(editorService.getSelectedLines()).toBe("mySelectedRange");
  });

  it("getSelectedLines returns null if editor is not initialized", () => {
    const editorService = new EditorService();
    expect(editorService.getSelectedLines()).toBeNull();
    editorService.editor = editorMock as any;
    expect(editorService.getSelectedLines()).toBeNull();
    editorService.editor = undefined;
    editorService.editorModel = editorModelMock as any;
    expect(editorService.getSelectedLines()).toBeNull();
  });

  it("setOnDidChangeContentListener", () => {
    const editorService = new EditorService();
    editorService.editor = editorMock as any;
    editorService.editorModel = editorModelMock as any;
    const mockFn = vi.fn();
    editorService.setOnDidChangeContentListener(mockFn);
    expect(editorModelMock.onDidChangeContent).toHaveBeenCalled();
  });

  describe("pasteToEditor", () => {
    it("pasteToEditor calls executeEdits", () => {
      const editorService = new EditorService();
      editorService.initEditorService({
        editor: editorMock as any,
        editorModel: editorModelMock as any,
      });
      editorService.pasteToEditor("my text");
      expect(editorMock.executeEdits).toHaveBeenCalledWith("", [
        {
          range: expect.anything(),
          text: "my text",
        },
      ]);
    });

    it("pasteToEditor uses selection per default", () => {
      const editorService = new EditorService();
      editorService.initEditorService({
        editor: editorMock as any,
        editorModel: editorModelMock as any,
      });
      editorService.pasteToEditor("my text");
      expect(editorMock.getSelection).toHaveBeenCalled();
      expect(editorMock.executeEdits).toHaveBeenCalledWith("", [
        {
          range: editorMock.getSelection(),
          text: "my text",
        },
      ]);
    });

    it("pasteToEditor uses position if selection is not available", () => {
      const editorService = new EditorService();
      editorMock.getSelection = vi.fn(() => {
        return null;
      }) as any;
      editorService.initEditorService({
        editor: editorMock as any,
        editorModel: editorModelMock as any,
      });
      editorService.pasteToEditor("my text");
      expect(editorMock.getSelection).toHaveBeenCalled();
      expect(editorMock.getPosition).toHaveBeenCalled();
      expect(editorMock.executeEdits).toHaveBeenCalledWith("", [
        {
          range: {
            startColumn: 456,
            endColumn: 456,
            startLineNumber: 123,
            endLineNumber: 123,
          },
          text: "my text",
        },
      ]);
    });

    it("paste to editor does nothing if neither selection nor position are available", () => {
      const editorService = new EditorService();
      editorMock.getSelection = vi.fn(() => null) as any;
      editorMock.getPosition = vi.fn(() => null) as any;
      editorService.initEditorService({
        editor: editorMock as any,
        editorModel: editorModelMock as any,
      });
      editorService.pasteToEditor("my text");
      expect(editorMock.getSelection).toHaveBeenCalled();
      expect(editorMock.getPosition).toHaveBeenCalled();
      expect(editorMock.executeEdits).not.toHaveBeenCalled();
    });
  });
});
