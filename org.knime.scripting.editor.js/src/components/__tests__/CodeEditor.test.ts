import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import CodeEditor from "../CodeEditor.vue";
import * as monaco from "monaco-editor";

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

describe("CodeEditor", () => {
  it("creates the editor model with the props", () => {
    const initialScript = "myInitialScript";
    const language = "javascript";
    const fileName = "myFileName.ext";
    mount(CodeEditor, {
      props: { initialScript, language, fileName },
    });
    expect(monaco.editor.createModel).toHaveBeenCalledWith(
      initialScript,
      language,
      `inmemory://${fileName}`,
    );
  });

  it("calls editor create with the ref", () => {
    const wrapper = mount(CodeEditor);
    expect(monaco.editor.create).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        model: "myModel",
      }),
    );
    expect(wrapper.text()).toContain("SCRIPTING EDITOR MOCK");
  });

  it("emits an event when the editor is created", () => {
    const wrapper = mount(CodeEditor);
    expect(wrapper.emitted()).toHaveProperty("monaco-created", [
      [{ editor: "myEditor", editorModel: "myModel" }],
    ]);
  });

  it("disposes the editor and model on unmount", async () => {
    const editorModelInstance = { dispose: vi.fn() };
    // @ts-ignore createModel is a mock
    monaco.editor.createModel.mockReturnValue(editorModelInstance);

    const editorInstance = { dispose: vi.fn() };
    // @ts-ignore create is a mock
    monaco.editor.create.mockReturnValue(editorInstance);

    // Mount the editor
    const wrapper = mount(CodeEditor);
    await flushPromises();

    // Unmount and expect the dispose methods to have been called
    wrapper.unmount();
    expect(editorInstance.dispose).toHaveBeenCalledOnce();
    expect(editorModelInstance.dispose).toHaveBeenCalledOnce();
  });
});
