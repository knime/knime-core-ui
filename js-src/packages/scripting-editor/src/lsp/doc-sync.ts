import type { editor } from "monaco-editor";
import type {
  DidChangeTextDocumentParams,
  DidOpenTextDocumentParams,
  TextDocumentContentChangeEvent,
  TextDocumentSyncClientCapabilities,
} from "vscode-languageserver-protocol";

import { mapDocumentIdToLSP, mapRangeToLSP } from "./mapping-utils";

export const documentSyncCapabilities: TextDocumentSyncClientCapabilities = {
  dynamicRegistration: false,
  willSave: false,
  willSaveWaitUntil: false,
};

const mapContentChangeToLSP = (
  change: editor.IModelContentChange,
): TextDocumentContentChangeEvent => ({
  range: mapRangeToLSP(change.range),
  text: change.text,
});

export const getDidOpenParams = (
  model: editor.ITextModel,
): DidOpenTextDocumentParams => ({
  textDocument: {
    ...mapDocumentIdToLSP(model),
    languageId: model.getLanguageId(),
    version: model.getVersionId(),
    text: model.getValue(),
  },
});

export const getDidChangeParams = (
  model: editor.ITextModel,
  changeEvent: editor.IModelContentChangedEvent,
): DidChangeTextDocumentParams => {
  return {
    textDocument: {
      ...mapDocumentIdToLSP(model),
      version: changeEvent.versionId,
    },
    contentChanges: changeEvent.changes.map(mapContentChangeToLSP),
  };
};
