import { describe, it, expect, vi, afterEach } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
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

vi.mock("@/scripting-service", () => {
  return {
    getScriptingService: vi.fn(() => ({
      getInitialSettings: vi.fn(() =>
        Promise.resolve({ script: "myInitialScript" }),
      ),
    })),
  };
});

describe("CodeEditor", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("creates the editor model with the props", async () => {
    const language = "javascript";
    const fileName = "myFileName.ext";
    mount(CodeEditor, {
      props: { language, fileName },
    });
    await flushPromises();
    expect(monaco.editor.createModel).toHaveBeenCalledWith(
      "myInitialScript",
      language,
      `inmemory://${fileName}`,
    );
  });

  it("calls editor create with the ref", async () => {
    const wrapper = mount(CodeEditor);
    await flushPromises();
    expect(monaco.editor.create).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        model: "myModel",
      }),
    );
    expect(wrapper.text()).toContain("SCRIPTING EDITOR MOCK");
  });

  it("emits an event when the editor is created", async () => {
    const wrapper = mount(CodeEditor);
    await flushPromises();
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
