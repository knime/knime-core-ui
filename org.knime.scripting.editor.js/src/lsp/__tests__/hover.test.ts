import { describe, expect, it } from "vitest";
import { Position, editor } from "monaco-editor";

import { getHoverParams, mapHoverResult } from "../hover";

describe("hover", () => {
  describe("getHoverParams", () => {
    const editorModel = {
      uri: "file:///tmp/file.txt",
    } as unknown as editor.ITextModel; // We just mock what we need
    const position = (lineNumber = 1, column = 1) =>
      ({ lineNumber, column }) as unknown as Position;

    it("maps uri", () => {
      const hoverParams = getHoverParams(editorModel, position());
      expect(hoverParams.textDocument.uri).toBe("file:///tmp/file.txt");
    });

    it("maps position", () => {
      const hoverParams = getHoverParams(editorModel, position(20, 10));
      expect(hoverParams.position.line).toBe(19);
      expect(hoverParams.position.character).toBe(9);
    });
  });

  describe("getHoverResult", () => {
    it("string", () => {
      const hoverResult = mapHoverResult({ contents: "string content" });
      expect(hoverResult).toEqual({
        contents: [
          { value: "string content", isTrusted: false, supportHtml: false },
        ],
      });
    });

    it("marked string", () => {
      const hoverResult = mapHoverResult({
        contents: { language: "lang", value: "foo" },
      });
      expect(hoverResult).toEqual({
        contents: [
          {
            value: "```lang\nfoo\n```",
            isTrusted: false,
            supportHtml: false,
          },
        ],
      });
    });

    it("marked string array", () => {
      const hoverResult = mapHoverResult({
        contents: ["first", { language: "lang", value: "foo" }, "last"],
      });
      expect(hoverResult).toEqual({
        contents: [
          { value: "first", isTrusted: false, supportHtml: false },
          {
            value: "```lang\nfoo\n```",
            isTrusted: false,
            supportHtml: false,
          },
          { value: "last", isTrusted: false, supportHtml: false },
        ],
      });
    });

    it("markup content (plaintext)", () => {
      const hoverResult = mapHoverResult({
        contents: { kind: "plaintext", value: "foo" },
      });
      expect(hoverResult).toEqual({
        contents: [{ value: "foo", isTrusted: false, supportHtml: false }],
      });
    });

    it("markup content (markdown)", () => {
      const hoverResult = mapHoverResult({
        contents: { kind: "markdown", value: "foo" },
      });
      expect(hoverResult).toEqual({
        contents: [{ value: "foo", isTrusted: false, supportHtml: false }],
      });
    });

    it("range", () => {
      const hoverResult = mapHoverResult({
        contents: "string content",
        range: {
          start: {
            line: 20,
            character: 12,
          },
          end: {
            line: 30,
            character: 15,
          },
        },
      });
      expect(hoverResult.range?.startLineNumber).toBe(21);
      expect(hoverResult.range?.startColumn).toBe(13);
      expect(hoverResult.range?.endLineNumber).toBe(31);
      expect(hoverResult.range?.endColumn).toBe(16);
    });
  });
});
