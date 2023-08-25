import { editor, languages } from "monaco-editor";
import {
  CompletionRequest,
  CompletionResolveRequest,
  DidChangeConfigurationNotification,
  DidChangeTextDocumentNotification,
  DidOpenTextDocumentNotification,
  HoverRequest,
  InitializeRequest,
  InitializedNotification,
  MessageReader,
  MessageWriter,
  PublishDiagnosticsNotification,
  SignatureHelpRequest,
  createMessageConnection,
  type CompletionOptions,
  type InitializeParams,
  type LSPAny,
  type MessageConnection,
  type SignatureHelpOptions,
} from "vscode-languageserver-protocol";

import {
  completionCapibilities,
  getCompletionParams,
  getCompletionResolveParams,
  mapCompletionResolveResult,
  mapCompletionResult,
} from "./completion";
import {
  mapDiagnosticToMarkerData,
  publishDiagnosticsCapibilities,
} from "./diagnostics";
import {
  documentSyncCapabilities,
  getDidChangeParams,
  getDidOpenParams,
} from "./doc-sync";
import { getHoverParams, hoverCapibilities, mapHoverResult } from "./hover";
import {
  getSignatureHelpParams,
  mapSignatureHelpResult,
  signatureHelpClientCapabilities,
} from "./signature-help";

export class MonacoLSPConnection {
  /**
   * Create a new connection to a language server.
   *
   * @param editorModel the editor model that holds the open document
   * @param reader a message reader for communication with the language server
   * @param writer a message writer for communication with the language server
   * @returns a fully initialized connection
   */
  static async create(
    editorModel: editor.ITextModel,
    reader: MessageReader,
    writer: MessageWriter,
  ) {
    const connection = new MonacoLSPConnection(editorModel, reader, writer);
    const initResponse = await connection.initializeLanguageServer();

    // Start the document synchronization
    connection.syncDocument();

    // -- Register Providers (client sends the request)

    // Hover
    const hoverOptions = initResponse.capabilities.hoverProvider;
    if (hoverOptions) {
      connection.registerHoverProvider();
    }

    // Auto-completion
    const completionOptions = initResponse.capabilities.completionProvider;
    if (completionOptions) {
      connection.registerCompletionProvider(completionOptions);
    }

    // Signature help
    const signatureHelpOptions =
      initResponse.capabilities.signatureHelpProvider;
    if (signatureHelpOptions) {
      connection.registerSignatureHelpProvider(signatureHelpOptions);
    }

    // -- Listen to Notifications (server sends the request)

    // Diagnstics
    connection.registerOnPublishDiagnostic();

    // -- Notify the server that the client is ready
    await connection.notifyInitialized();

    return connection;
  }

  private _connection: MessageConnection;

  private constructor(
    private _editorModel: editor.ITextModel,
    reader: MessageReader,
    writer: MessageWriter,
  ) {
    this._connection = createMessageConnection(reader, writer);
    this._connection.listen();
  }

  /**
   * Change the configuration of the language server.
   * Can be called anytime to update the current configuration.
   *
   * @param settings the settings that are sent to the server
   */
  async changeConfiguration(settings: LSPAny) {
    await this._connection.sendNotification(
      DidChangeConfigurationNotification.type,
      { settings },
    );
  }

  private getLanguageId() {
    return this._editorModel.getLanguageId();
  }

  /**
   * Lifecyle - Initialize Request
   */
  private initializeLanguageServer() {
    // Send the initialization request
    const initParams: InitializeParams = {
      processId: null,
      capabilities: {
        textDocument: {
          hover: hoverCapibilities,
          completion: completionCapibilities,
          publishDiagnostics: publishDiagnosticsCapibilities,
          signatureHelp: signatureHelpClientCapabilities,
          synchronization: documentSyncCapabilities,
        },
        workspace: {
          configuration: false, // not implemented
          didChangeConfiguration: {
            dynamicRegistration: false,
          },
        },
      },
      rootUri: null, // No folder open
    };
    return this._connection.sendRequest(InitializeRequest.type, initParams);
  }

  private async notifyInitialized() {
    await this._connection.sendNotification(InitializedNotification.type, {});
  }

  /**
   * Document synchronization
   */
  private syncDocument() {
    // Send the first version of the document
    this._connection.sendNotification(
      DidOpenTextDocumentNotification.type,
      getDidOpenParams(this._editorModel),
    );

    // Listen on document changes and send them to the server
    this._editorModel.onDidChangeContent((event) => {
      this._connection.sendNotification(
        DidChangeTextDocumentNotification.type,
        getDidChangeParams(this._editorModel, event),
      );
    });
  }

  /**
   * Hover
   */
  private registerHoverProvider() {
    languages.registerHoverProvider(this.getLanguageId(), {
      provideHover: async (model, position) => {
        const result = await this._connection.sendRequest(
          HoverRequest.type,
          getHoverParams(model, position),
        );
        if (result === null) {
          return null;
        }
        return mapHoverResult(result);
      },
    });
  }

  /**
   * Auto-completion
   */
  private registerCompletionProvider(options: CompletionOptions) {
    languages.registerCompletionItemProvider(this.getLanguageId(), {
      triggerCharacters: options.triggerCharacters,
      provideCompletionItems: async (model, position, context) => {
        const result = await this._connection.sendRequest(
          CompletionRequest.type,
          getCompletionParams(model, position, context),
        );
        if (result === null) {
          return null;
        }
        return mapCompletionResult(result, model, position);
      },
      resolveCompletionItem: async (item) => {
        const result = await this._connection.sendRequest(
          CompletionResolveRequest.type,
          getCompletionResolveParams(item),
        );
        return mapCompletionResolveResult(result, item.range);
      },
    });
  }

  /**
   * Signature help
   */
  private registerSignatureHelpProvider(options: SignatureHelpOptions) {
    languages.registerSignatureHelpProvider(this.getLanguageId(), {
      signatureHelpTriggerCharacters: options.triggerCharacters,
      signatureHelpRetriggerCharacters: options.retriggerCharacters,
      provideSignatureHelp: async (model, position, _token, context) => {
        const result = await this._connection.sendRequest(
          SignatureHelpRequest.type,
          getSignatureHelpParams(model, position, context),
        );
        if (result === null) {
          return null;
        }
        return mapSignatureHelpResult(result);
      },
    });
  }

  /**
   * Diagnostics
   */
  private registerOnPublishDiagnostic() {
    this._connection.onNotification(
      PublishDiagnosticsNotification.type,
      (p) => {
        editor.setModelMarkers(
          this._editorModel,
          "lsp-diagnostics",
          mapDiagnosticToMarkerData(p.diagnostics),
        );
      },
    );
  }
}
