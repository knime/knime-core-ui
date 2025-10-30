import { describe, expect, it, vi } from "vitest";
import { Position, editor, languages } from "monaco-editor";
import {
  CompletionItemKind,
  CompletionItemTag,
  CompletionTriggerKind,
  InsertTextMode,
} from "vscode-languageserver-protocol";

import {
  getCompletionParams,
  getCompletionResolveParams,
  mapCompletionResult,
} from "../completion";

vi.hoisted(() => {
  vi.resetModules();
  vi.doUnmock("monaco-editor");
});

describe("completion", () => {
  const editorModel = {
    uri: "file:///tmp/file.txt",
    getWordUntilPosition: (position: Position) => ({
      word: "foo",
      startColumn: position.column - 1,
      endColumn: position.column + 2,
    }),
  } as unknown as editor.ITextModel; // We just mock what we need
  const position = (lineNumber = 1, column = 1) =>
    ({ lineNumber, column }) as unknown as Position;

  describe("getCompletionParams", () => {
    const context = (
      triggerKind = languages.CompletionTriggerKind.Invoke,
      triggerCharacter?: string,
    ) => ({
      triggerKind,
      triggerCharacter,
    });

    it("maps uri", () => {
      const params = getCompletionParams(editorModel, position(), context());
      expect(params.textDocument.uri).toBe("file:///tmp/file.txt");
    });

    it("maps position", () => {
      const params = getCompletionParams(
        editorModel,
        position(20, 10),
        context(),
      );
      expect(params.position.line).toBe(19);
      expect(params.position.character).toBe(9);
    });

    describe("maps context", () => {
      it("invoke trigger kind", () => {
        const params = getCompletionParams(
          editorModel,
          position(),
          context(languages.CompletionTriggerKind.Invoke, undefined),
        );
        expect(params.context?.triggerKind).toBe(CompletionTriggerKind.Invoked);
        expect(params.context?.triggerCharacter).toBeUndefined();
      });

      it("trigger character trigger kind", () => {
        const params = getCompletionParams(
          editorModel,
          position(),
          context(languages.CompletionTriggerKind.TriggerCharacter, "a"),
        );
        expect(params.context?.triggerKind).toBe(
          CompletionTriggerKind.TriggerCharacter,
        );
        expect(params.context?.triggerCharacter).toBe("a");
      });

      it("incomplet completions trigger kind", () => {
        const params = getCompletionParams(
          editorModel,
          position(),
          context(
            languages.CompletionTriggerKind.TriggerForIncompleteCompletions,
          ),
        );
        expect(params.context?.triggerKind).toBe(
          CompletionTriggerKind.TriggerForIncompleteCompletions,
        );
        expect(params.context?.triggerCharacter).toBeUndefined();
      });
    });
  });

  describe("mapCompletionResult", () => {
    it("gets range", () => {
      const result = mapCompletionResult(
        [{ label: "foo" }],
        editorModel,
        position(10, 4),
      );
      expect(result.suggestions[0].range).toEqual({
        startLineNumber: 10,
        startColumn: 3,
        endLineNumber: 10,
        endColumn: 6,
      });
    });

    it("maps completion item array", () => {
      const result = mapCompletionResult(
        [{ label: "foo" }, { label: "bar" }],
        editorModel,
        position(),
      );
      expect(
        (result.suggestions[0].label as languages.CompletionItemLabel).label,
      ).toBe("foo");
      expect(
        (result.suggestions[1].label as languages.CompletionItemLabel).label,
      ).toBe("bar");
    });

    it("maps incomplete completion item list", () => {
      const result = mapCompletionResult(
        { items: [{ label: "foo" }, { label: "bar" }], isIncomplete: true },
        editorModel,
        position(),
      );
      expect(
        (result.suggestions[0].label as languages.CompletionItemLabel).label,
      ).toBe("foo");
      expect(
        (result.suggestions[1].label as languages.CompletionItemLabel).label,
      ).toBe("bar");
      expect(result.incomplete).toBe(true);
    });

    it("maps complete completion item list", () => {
      const result = mapCompletionResult(
        { items: [{ label: "foo" }, { label: "bar" }], isIncomplete: false },
        editorModel,
        position(),
      );
      expect(
        (result.suggestions[0].label as languages.CompletionItemLabel).label,
      ).toBe("foo");
      expect(
        (result.suggestions[1].label as languages.CompletionItemLabel).label,
      ).toBe("bar");
      expect(result.incomplete).toBe(false);
    });

    it("maps completion item", () => {
      const result = mapCompletionResult(
        [
          {
            label: "foo",
            kind: CompletionItemKind.Function,
            tags: [CompletionItemTag.Deprecated],
            detail: "detail",
            documentation: "documentation",
            sortText: "sortText",
            filterText: "filterText",
            preselect: true,
            insertTextMode: InsertTextMode.adjustIndentation,
            commitCharacters: ["a", "b"],
          },
        ],
        editorModel,
        position(),
      );
      const resultItem = result.suggestions[0];
      expect((resultItem.label as languages.CompletionItemLabel).label).toBe(
        "foo",
      );
      expect(resultItem.kind).toBe(languages.CompletionItemKind.Function);
      expect(resultItem.tags).toEqual([languages.CompletionItemTag.Deprecated]);
      expect(resultItem.detail).toBe("detail");
      expect(resultItem.documentation).toEqual(
        expect.objectContaining({ value: "documentation" }),
      );
      expect(resultItem.sortText).toBe("sortText");
      expect(resultItem.filterText).toBe("filterText");
      expect(resultItem.preselect).toBe(true);
      expect(resultItem.insertTextRules).toBe(
        languages.CompletionItemInsertTextRule.KeepWhitespace,
      );
      expect(resultItem.commitCharacters).toEqual(["a", "b"]);
    });
  });

  describe("mapCompletionResolveParams", () => {
    it("maps string label", () => {
      const params = getCompletionResolveParams({
        label: "foo",
      } as unknown as languages.CompletionItem);
      expect(params.label).toBe("foo");
    });

    it("maps completion item label", () => {
      const params = getCompletionResolveParams({
        label: { label: "foo" },
      } as unknown as languages.CompletionItem);
      expect(params.label).toBe("foo");
    });

    it("keeps data", () => {
      const params = getCompletionResolveParams({
        label: "",
        data: "myData",
      } as unknown as languages.CompletionItem);
      expect(params.data).toBe("myData");
    });
  });
});
