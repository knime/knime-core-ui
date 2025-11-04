import {
  type IMarkdownString,
  type IRange,
  type Position,
  editor,
} from "monaco-editor";
import {
  Position as LSPPosition,
  MarkupContent,
  Range,
  TextDocumentIdentifier,
  type TextDocumentPositionParams,
} from "vscode-languageserver-protocol";

// Position

export const mapPositionToLSP = (position: Position): LSPPosition => {
  return {
    line: position.lineNumber - 1,
    character: position.column - 1,
  };
};

// Markup

export const mapMarkupToMonaco = (
  markup: MarkupContent | string,
): IMarkdownString => {
  let value: string;
  if (typeof markup === "string") {
    value = markup;
  } else {
    value = markup.value;
  }
  return {
    value,
    isTrusted: false,
    supportHtml: false,
  };
};

// Range

export const mapRangeToMonaco = (range: Range): IRange => {
  return {
    startLineNumber: range.start.line + 1,
    startColumn: range.start.character + 1,
    endLineNumber: range.end.line + 1,
    endColumn: range.end.character + 1,
  };
};

export const mapRangeToLSP = (range: IRange): Range => {
  return {
    start: {
      line: range.startLineNumber - 1,
      character: range.startColumn - 1,
    },
    end: {
      line: range.endLineNumber - 1,
      character: range.endColumn - 1,
    },
  };
};

// Document Id

export const mapDocumentIdToLSP = (
  editorModel: editor.ITextModel,
): TextDocumentIdentifier => {
  return {
    uri: editorModel.uri.toString(),
  };
};

// Combined parameters

export const getTextDocPositionParms = (
  editorModel: editor.ITextModel,
  position: Position,
): TextDocumentPositionParams => {
  return {
    textDocument: mapDocumentIdToLSP(editorModel),
    position: mapPositionToLSP(position),
  };
};
