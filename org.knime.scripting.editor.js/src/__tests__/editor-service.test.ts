import { EditorService } from "@/editor-service";
import { editor } from "__mocks__/monaco-editor";
import { describe, expect, it, vi } from "vitest";

vi.mock("monaco-editor");

const editorMock = editor.create({ innerHTML: "" } as any);
const editorModelMock = editor.createModel();

describe("editor-service", () => {
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
});
