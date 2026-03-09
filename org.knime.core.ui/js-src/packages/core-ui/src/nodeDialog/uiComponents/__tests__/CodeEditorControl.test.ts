import {
  type Mock,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import type { VueWrapper } from "@vue/test-utils";

import {
  type VueControlTestProps,
  getControlBase,
  mountJsonFormsControlLabelContent,
} from "@knime/jsonforms/testing";

import CodeEditorControl from "../CodeEditorControl.vue";

const mockGetValue = vi.fn().mockReturnValue("");
const mockSetValue = vi.fn();
const mockDispose = vi.fn();
const mockOnDidChangeModelContent = vi.fn();
const mockUpdateOptions = vi.fn();

vi.mock("monaco-editor", () => ({
  editor: {
    create: vi.fn(() => ({
      getModel: () => ({
        getValue: mockGetValue,
        setValue: mockSetValue,
      }),
      onDidChangeModelContent: mockOnDidChangeModelContent,
      updateOptions: mockUpdateOptions,
      dispose: mockDispose,
    })),
  },
}));

describe("CodeEditorControl.vue", () => {
  let props: VueControlTestProps<typeof CodeEditorControl>;
  let wrapper: VueWrapper;
  let changeValue: Mock;
  const labelForId = "myLabelForId";

  beforeEach(() => {
    vi.clearAllMocks();
    props = {
      control: {
        ...getControlBase("codeEditor"),
        data: '{"key": "value"}',
        schema: {
          type: "string",
        },
        uischema: {
          type: "Control",
          scope: "#/properties/view/properties/codeEditor",
          options: {
            format: "codeEditor",
            language: "json",
          },
        },
      },
    };
    ({ wrapper, changeValue } = mountJsonFormsControlLabelContent(
      CodeEditorControl,
      { props },
    ));
  });

  it("renders a container element", () => {
    expect(wrapper.find(".code-editor-container").exists()).toBe(true);
  });

  it("sets labelForId on the container", () => {
    expect(wrapper.find(".code-editor-container").attributes("id")).toBe(
      labelForId,
    );
  });

  it("creates monaco editor with correct options", async () => {
    const { editor } = await import("monaco-editor");
    expect(editor.create).toHaveBeenCalledWith(
      expect.any(HTMLDivElement),
      expect.objectContaining({
        value: '{"key": "value"}',
        language: "json",
        automaticLayout: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        readOnly: false,
      }),
    );
  });

  it("registers onDidChangeModelContent callback", () => {
    expect(mockOnDidChangeModelContent).toHaveBeenCalledWith(
      expect.any(Function),
    );
  });

  it("calls changeValue when editor content changes", () => {
    const callback = mockOnDidChangeModelContent.mock.calls[0][0];
    mockGetValue.mockReturnValue("new content");
    callback();
    expect(changeValue).toHaveBeenCalledWith("new content");
  });

  it("does not call changeValue when editor content matches current data", () => {
    const callback = mockOnDidChangeModelContent.mock.calls[0][0];
    mockGetValue.mockReturnValue('{"key": "value"}');
    callback();
    expect(changeValue).not.toHaveBeenCalled();
  });

  it("disposes editor on unmount", () => {
    wrapper.unmount();
    expect(mockDispose).toHaveBeenCalled();
  });
});
