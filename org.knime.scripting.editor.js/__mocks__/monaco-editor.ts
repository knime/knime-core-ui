import { vi } from "vitest";

export const editor = {
  createModel: vi.fn(() => ({
    getValue: vi.fn(() => "myInitialScript"),
    getLineLastNonWhitespaceColumn: vi.fn(() => 100),
    getValueInRange: vi.fn(() => "mySelectedRange"),
    getPosition: vi.fn(() => "myPosition"),
    onDidChangeContent: vi.fn(),
  })),
  create: vi.fn((element: HTMLElement) => {
    element.innerHTML = "SCRIPTING EDITOR MOCK";
    return {
      name: "myEditor",
      getSelection: vi.fn(() => ({})),
      executeEdits: vi.fn(() => {}),
      pushUndoStop: vi.fn(() => {}),
      getPosition: vi.fn(() => ({
        lineNumber: 123,
        column: 456,
      })),
      onDidChangeCursorSelection: vi.fn(),
    };
  }),
  createDiffEditor: vi.fn((element: HTMLElement) => {
    element.innerHTML = "DIFF EDITOR MOCK";
    return {
      getModifiedEditor: vi.fn(() => ({
        getValue: vi.fn(() => {
          "myDiffScript";
        }),
      })),
      setModel: vi.fn(() => {}),
    };
  }),
};

export const Uri = {
  parse: vi.fn((path: string) => path),
};

export const languages = {
  registerHoverProvider: vi.fn(),
  registerCompletionItemProvider: vi.fn(),
};
