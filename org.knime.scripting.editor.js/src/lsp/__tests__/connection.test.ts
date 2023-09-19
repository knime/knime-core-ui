import {
  Position,
  editor,
  languages,
  type CancellationToken,
} from "monaco-editor";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  MessageReader,
  MessageWriter,
  type DataCallback,
  type InitializeResult,
  type PublishDiagnosticsParams,
} from "vscode-languageserver-protocol";
import sleep from "webapps-common/util/sleep";

import { MonacoLSPConnection } from "../connection";

import * as completion from "../completion";
import * as diagnostics from "../diagnostics";
import * as docSync from "../doc-sync";
import * as hover from "../hover";
import * as signatureHelp from "../signature-help";
import { nextTick } from "vue";

// Install spies on monaco APIs
vi.spyOn(languages, "registerHoverProvider");
vi.spyOn(languages, "registerCompletionItemProvider");
vi.spyOn(languages, "registerSignatureHelpProvider");
vi.spyOn(editor, "setModelMarkers");

// Install spies on mapper API
vi.spyOn(hover, "getHoverParams");
vi.spyOn(hover, "mapHoverResult");
vi.spyOn(completion, "getCompletionParams");
vi.spyOn(completion, "getCompletionResolveParams");
vi.spyOn(completion, "mapCompletionResult");
vi.spyOn(completion, "mapCompletionResolveResult");
vi.spyOn(signatureHelp, "getSignatureHelpParams");
vi.spyOn(signatureHelp, "mapSignatureHelpResult");

const cancelToken = null as unknown as CancellationToken;

describe("monaco-lsp", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  const position = { lineNumber: 1, column: 1 } as unknown as Position;
  const createConnection = async (
    initResponse: InitializeResult = { capabilities: {} },
  ) => {
    const editorModel = {
      onDidChangeContent: vi.fn(),
      uri: "inmemory://foo",
      getLanguageId: () => "languageId",
      getValue: () => "my script",
      getVersionId: () => 0,
      getWordUntilPosition: () => ({ startColumn: 0, endColumn: 0 }),
    } as unknown as editor.ITextModel; // We just mock what we need

    let resolveCallback: (value: (data: any) => void) => void;
    const callbackPromise = new Promise<(data: any) => void>((resolve) => {
      resolveCallback = resolve;
    });

    const reader = {
      onClose: vi.fn(),
      onError: vi.fn(),
      listen(callback: DataCallback) {
        resolveCallback(callback);
      },
    } as unknown as MessageReader;
    const writer = {
      onClose: vi.fn(),
      onError: vi.fn(),
      write: vi.fn(() => Promise.resolve()),
    } as unknown as MessageWriter;
    const connectionPromise = MonacoLSPConnection.create(
      editorModel,
      reader,
      writer,
    );
    const sendFromServer = await callbackPromise;
    const resolveLastServerRequest = (result: any) =>
      sendFromServer({
        jsonrpc: "2.0",
        // @ts-ignore - write is a mock and has been called
        id: writer.write.mock.lastCall[0].id,
        result,
      });

    // Respond to the initialize request
    resolveLastServerRequest(initResponse);

    return {
      connection: await connectionPromise,
      sendFromServer,
      resolveLastServerRequest,
      editorModel,
      reader,
      writer,
    };
  };

  describe("initialize", () => {
    it("send initialize request", async () => {
      const { writer } = await createConnection();
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "initialize",
          params: {
            processId: null,
            rootUri: null,
            capabilities: {
              textDocument: {
                hover: hover.hoverCapibilities,
                completion: completion.completionCapibilities,
                publishDiagnostics: diagnostics.publishDiagnosticsCapibilities,
                signatureHelp: signatureHelp.signatureHelpClientCapabilities,
                synchronization: docSync.documentSyncCapabilities,
              },
              workspace: {
                configuration: false, // not implemented
                didChangeConfiguration: {
                  dynamicRegistration: false,
                },
              },
            },
          },
        }),
      );
    });

    it("send initialized notification", async () => {
      const { writer } = await createConnection();
      expect(writer.write).toHaveBeenCalledWith({
        jsonrpc: "2.0",
        method: "initialized",
        params: {},
      });
    });
  });

  it("change configuration", async () => {
    const { connection, writer } = await createConnection();
    const settings = { mySettings: "hello" };
    await connection.changeConfiguration(settings);
    expect(writer.write).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "workspace/didChangeConfiguration",
        params: { settings },
      }),
    );
  });

  describe("document sync", () => {
    it("sends did open", async () => {
      const { writer, editorModel } = await createConnection();
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "textDocument/didOpen",
          params: docSync.getDidOpenParams(editorModel),
        }),
      );
    });

    it("updates on change", async () => {
      const { writer, editorModel } = await createConnection();
      expect(editorModel.onDidChangeContent).toHaveBeenCalled();

      const changeListener = vi.mocked(editorModel.onDidChangeContent).mock
        .calls[0][0];
      const event = {
        changes: [],
      } as unknown as editor.IModelContentChangedEvent;
      changeListener(event);

      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "textDocument/didChange",
          params: docSync.getDidChangeParams(editorModel, event),
        }),
      );
    });
  });

  describe("hover provider", () => {
    const hoverServerCapabilities = {
      capabilities: {
        hoverProvider: true,
      },
    };
    const getHoverProvider = () => {
      expect(languages.registerHoverProvider).toHaveBeenCalled();
      return vi.mocked(languages.registerHoverProvider).mock.calls[0][1]
        .provideHover;
    };

    it("does not register if server has no capability", async () => {
      await createConnection();
      expect(languages.registerHoverProvider).not.toHaveBeenCalled();
    });

    it("registers if server has capability", async () => {
      await createConnection(hoverServerCapabilities);
      expect(languages.registerHoverProvider).toHaveBeenCalledWith(
        "languageId",
        expect.objectContaining({
          provideHover: expect.any(Function),
        }),
      );
    });

    it("calls server on hover request", async () => {
      const { editorModel, writer } = await createConnection(
        hoverServerCapabilities,
      );

      getHoverProvider()(editorModel, position, cancelToken);
      await nextTick();
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "textDocument/hover",
        }),
      );
      expect(hover.getHoverParams).toHaveBeenCalledWith(editorModel, position);
    });

    it("handles null hover result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        hoverServerCapabilities,
      );

      const promise = getHoverProvider()(editorModel, position, cancelToken);
      await nextTick();
      resolveLastServerRequest(null);
      expect(await promise).toBeNull();
    });

    it("maps hover result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        hoverServerCapabilities,
      );

      const promise = getHoverProvider()(editorModel, position, cancelToken);
      await nextTick();
      resolveLastServerRequest({ contents: "foo" });
      const result = await promise;
      expect(hover.mapHoverResult).toHaveBeenCalledWith({ contents: "foo" });
      expect(result).toBe(
        vi.mocked(hover.mapHoverResult).mock.results[0].value,
      );
    });
  });

  describe("completion provider", () => {
    const completionServerCapabilities = {
      capabilities: {
        completionProvider: {
          triggerCharacters: ["."],
        },
      },
    };
    const getCompletionProvider = () => {
      expect(languages.registerCompletionItemProvider).toHaveBeenCalled();
      return vi.mocked(languages.registerCompletionItemProvider).mock
        .calls[0][1];
    };
    const context = { triggerKind: 0 };
    const completionItem: languages.CompletionItem = {
      label: "foo",
      insertText: "foo",
      kind: 0,
      range: {
        startLineNumber: 1,
        startColumn: 1,
        endLineNumber: 1,
        endColumn: 1,
      },
    };

    it("does not register if server has no capability", async () => {
      await createConnection();
      expect(languages.registerCompletionItemProvider).not.toHaveBeenCalled();
    });

    it("registers if server has capability", async () => {
      await createConnection(completionServerCapabilities);
      expect(languages.registerCompletionItemProvider).toHaveBeenCalledWith(
        "languageId",
        expect.objectContaining({
          triggerCharacters: ["."],
          provideCompletionItems: expect.any(Function),
          resolveCompletionItem: expect.any(Function),
        }),
      );
    });

    it("calls server on completion request", async () => {
      const { editorModel, writer } = await createConnection(
        completionServerCapabilities,
      );

      getCompletionProvider().provideCompletionItems(
        editorModel,
        position,
        context,
        cancelToken,
      );
      await nextTick();
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "textDocument/completion",
        }),
      );
      expect(completion.getCompletionParams).toHaveBeenCalledWith(
        editorModel,
        position,
        context,
      );
    });

    it("calls server on completion resolve request", async () => {
      const { writer } = await createConnection(completionServerCapabilities);

      getCompletionProvider().resolveCompletionItem!(
        completionItem,
        cancelToken,
      );
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "completionItem/resolve",
        }),
      );
      expect(completion.getCompletionResolveParams).toHaveBeenCalledWith(
        completionItem,
      );
    });

    it("handles null completions result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        completionServerCapabilities,
      );

      const promise = getCompletionProvider().provideCompletionItems(
        editorModel,
        position,
        context,
        cancelToken,
      );
      await nextTick();
      resolveLastServerRequest(null);
      expect(await promise).toBeNull();
    });

    it("maps completion result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        completionServerCapabilities,
      );

      const promise = getCompletionProvider().provideCompletionItems(
        editorModel,
        position,
        context,
        cancelToken,
      );
      await nextTick();
      resolveLastServerRequest([{ label: "foo" }]);
      const result = await promise;
      expect(completion.mapCompletionResult).toHaveBeenCalledWith(
        [{ label: "foo" }],
        editorModel,
        position,
      );
      expect(result).toBe(
        vi.mocked(completion.mapCompletionResult).mock.results[0].value,
      );
    });

    it("maps completion resolve result", async () => {
      const { resolveLastServerRequest } = await createConnection(
        completionServerCapabilities,
      );

      const item: languages.CompletionItem = {
        label: "foo",
        insertText: "foo",
        kind: languages.CompletionItemKind.Text,
        range: {
          startLineNumber: 1,
          startColumn: 1,
          endLineNumber: 1,
          endColumn: 1,
        },
      };
      const promise = getCompletionProvider().resolveCompletionItem!(
        item,
        cancelToken,
      );
      await nextTick();
      resolveLastServerRequest({ label: "foo" });
      const result = await promise;
      expect(completion.mapCompletionResolveResult).toHaveBeenCalledWith(
        { label: "foo" },
        item.range,
      );
      expect(result).toBe(
        vi.mocked(completion.mapCompletionResolveResult).mock.results[0].value,
      );
    });
  });

  describe("signature help provider", () => {
    const signatureHelpServerCapabilities = {
      capabilities: {
        signatureHelpProvider: {
          triggerCharacters: ["a"],
          retriggerCharacters: ["b"],
        },
      },
    };
    const getSignatureHelpProvider = () => {
      expect(languages.registerSignatureHelpProvider).toHaveBeenCalled();
      return vi.mocked(languages.registerSignatureHelpProvider).mock
        .calls[0][1];
    };
    const context: languages.SignatureHelpContext = {
      isRetrigger: false,
      triggerKind: languages.SignatureHelpTriggerKind.Invoke,
    };

    it("does not register if server has no capability", async () => {
      await createConnection();
      expect(languages.registerSignatureHelpProvider).not.toHaveBeenCalled();
    });

    it("registers if server has capability", async () => {
      await createConnection(signatureHelpServerCapabilities);
      expect(languages.registerSignatureHelpProvider).toHaveBeenCalledWith(
        "languageId",
        {
          signatureHelpTriggerCharacters: ["a"],
          signatureHelpRetriggerCharacters: ["b"],
          provideSignatureHelp: expect.any(Function),
        },
      );
    });

    it("calls server on signature help request", async () => {
      const { editorModel, writer } = await createConnection(
        signatureHelpServerCapabilities,
      );

      getSignatureHelpProvider().provideSignatureHelp(
        editorModel,
        position,
        cancelToken,
        context,
      );
      await nextTick();
      expect(writer.write).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "textDocument/signatureHelp",
        }),
      );
      expect(signatureHelp.getSignatureHelpParams).toHaveBeenCalledWith(
        editorModel,
        position,
        context,
      );
    });

    it("handles null signature help result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        signatureHelpServerCapabilities,
      );

      const promise = getSignatureHelpProvider().provideSignatureHelp(
        editorModel,
        position,
        cancelToken,
        context,
      );
      await nextTick();
      resolveLastServerRequest(null);
      expect(await promise).toBeNull();
    });

    it("maps signature help result", async () => {
      const { editorModel, resolveLastServerRequest } = await createConnection(
        signatureHelpServerCapabilities,
      );

      const promise = getSignatureHelpProvider().provideSignatureHelp(
        editorModel,
        position,
        cancelToken,
        context,
      );
      const signatureHelpResult = { signatures: [{ label: "a" }] };
      await nextTick();
      resolveLastServerRequest(signatureHelpResult);
      const result = await promise;
      expect(signatureHelp.mapSignatureHelpResult).toHaveBeenCalledWith(
        signatureHelpResult,
      );
      expect(result).toBe(
        vi.mocked(signatureHelp.mapSignatureHelpResult).mock.results[0].value,
      );
    });
  });

  it("on publish diagnostic", async () => {
    const { editorModel, sendFromServer } = await createConnection();

    const params: PublishDiagnosticsParams = {
      diagnostics: [
        {
          range: {
            start: {
              line: 1,
              character: 1,
            },
            end: {
              line: 1,
              character: 1,
            },
          },
          message: "foo",
        },
      ],
      uri: editorModel.uri.toString(),
    };
    sendFromServer({
      jsonrpc: "2.0",
      method: "textDocument/publishDiagnostics",
      params,
    });

    await sleep(20);
    expect(editor.setModelMarkers).toHaveBeenCalledWith(
      editorModel,
      "lsp-diagnostics",
      diagnostics.mapDiagnosticToMarkerData(params.diagnostics),
    );
  });
});
