import { describe, expect, it } from "vitest";
import { editor } from "monaco-editor";
import type { Range } from "vscode-languageserver-protocol";

import { getDidChangeParams, getDidOpenParams } from "../doc-sync";

describe("doc-sync", () => {
  const editorModel = {
    uri: "file:///tmp/file.txt",
    getLanguageId: () => "myLanguageId",
    getVersionId: () => 0,
    getValue: () => "foo",
  } as unknown as editor.ITextModel; // We just mock what we need

  it("getDidOpenParams", () => {
    const didOpenParams = getDidOpenParams(editorModel);
    expect(didOpenParams.textDocument.uri).toBe("file:///tmp/file.txt");
    expect(didOpenParams.textDocument.languageId).toBe("myLanguageId");
    expect(didOpenParams.textDocument.version).toBe(0);
    expect(didOpenParams.textDocument.text).toBe("foo");
  });

  it("didChangeParams", () => {
    const didChangeParams = getDidChangeParams(editorModel, {
      versionId: 99,
      changes: [
        {
          range: {
            startLineNumber: 10,
            startColumn: 12,
            endLineNumber: 20,
            endColumn: 22,
          },
          text: "Bla",
        },
        {
          text: "Hello",
          range: {
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: 9,
            endColumn: 9,
          },
        },
      ],
    } as unknown as editor.IModelContentChangedEvent);
    expect(didChangeParams.textDocument.version).toBe(99);
    expect(didChangeParams.contentChanges[0].text).toBe("Bla");
    // @ts-ignore
    const change0Range: Range = didChangeParams.contentChanges[0].range;
    expect(change0Range.start.line).toBe(9);
    expect(change0Range.start.character).toBe(11);
    expect(change0Range.end.line).toBe(19);
    expect(change0Range.end.character).toBe(21);

    expect(didChangeParams.contentChanges[1].text).toBe("Hello");
    // @ts-ignore
    const change1Range: Range = didChangeParams.contentChanges[1].range;
    expect(change1Range.start.line).toBe(0);
    expect(change1Range.start.character).toBe(0);
    expect(change1Range.end.line).toBe(8);
    expect(change1Range.end.character).toBe(8);
  });
});
