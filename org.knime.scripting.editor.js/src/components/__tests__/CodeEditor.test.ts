import { describe, it, expect, vi, afterEach } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import CodeEditor from "../CodeEditor.vue";
import * as monaco from "monaco-editor";
import { getScriptingService } from "@/scripting-service";

vi.mock("monaco-editor");
vi.mock("@/scripting-service");

describe("CodeEditor", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("creates the editor model with the props", async () => {
    const language = "javascript";
    const fileName = "myFileName.ext";
    const initialScript = "myInitialScript";

    mount(CodeEditor, {
      props: { language, fileName, initialScript },
    });
    await flushPromises();
    expect(monaco.editor.createModel).toHaveBeenCalledWith(
      "myInitialScript",
      language,
      `inmemory://model/${fileName}`,
    );
  });

  it("creates the editor model with an empty script", async () => {
    const language = "javascript";
    const fileName = "myFileName.ext";
    const initialScript = "";

    mount(CodeEditor, {
      props: { language, fileName, initialScript },
    });
    await flushPromises();
    expect(monaco.editor.createModel).toHaveBeenCalledWith(
      "",
      language,
      `inmemory://model/${fileName}`,
    );
  });

  it("loads script from service if no initial script is provided", async () => {
    const language = "javascript";
    const fileName = "myFileName.ext";

    mount(CodeEditor, {
      props: { language, fileName },
    });
    await flushPromises();
    expect(monaco.editor.createModel).toHaveBeenCalledWith(
      "myInitialScript",
      language,
      `inmemory://model/${fileName}`,
    );
  });

  it("calls editor create with the ref", async () => {
    const wrapper = mount(CodeEditor);
    await flushPromises();
    expect(monaco.editor.create).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
    );
    expect(wrapper.text()).toContain("SCRIPTING EDITOR MOCK");
  });

  it("emits an event when the editor is created", async () => {
    const wrapper = mount(CodeEditor);
    await flushPromises();
    expect(wrapper.emitted()).toHaveProperty("monaco-created", [
      [
        {
          editor: expect.objectContaining({ name: "myEditor" }),
          editorModel: expect.anything(),
        },
      ],
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

  it("creates diff editor if diff script is set", async () => {
    mount(CodeEditor, {
      props: {
        initialScript: "my initial script",
        diffScript: "my diff script",
      },
    });
    await flushPromises();
    expect(monaco.editor.createDiffEditor).toHaveBeenCalledOnce();
    expect(monaco.editor.createModel).toHaveBeenCalledTimes(2);
  });

  it("loads initial script from service if it is not set", async () => {
    mount(CodeEditor);
    await flushPromises();
    expect(getScriptingService().getInitialSettings).toHaveBeenCalled();
  });

  it("does not load initial script from service if it is passed as a prop", async () => {
    mount(CodeEditor, {
      props: { initialScript: "my initial script" },
    });
    await flushPromises();
    expect(getScriptingService().getInitialSettings).not.toHaveBeenCalled();
  });
});
