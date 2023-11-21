import {
  IFrameKnimeService,
  JsonDataService,
} from "@knime/ui-extension-service";
import { editor as monaco, type IDisposable } from "monaco-editor";
import type { ConsoleText } from "./components/OutputConsole.vue";
import { EditorService } from "./editor-service";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import type { InputOutputModel } from "./components/InputOutputItem.vue";

export type NodeSettings = { script: string; scriptUsedFlowVariable?: string };
type LanugageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

class ScriptingService {
  private _knimeService: Promise<IFrameKnimeService>;
  private _jsonDataService: Promise<JsonDataService>;
  private _settings: NodeSettings | null = null;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};
  private _runEventPoller: boolean = true;
  private _editorService: EditorService = new EditorService();
  private _monacoLSPConnection: MonacoLSPConnection | null = null;
  protected _clearConsoleCallback?: Function;

  constructor() {
    const _createKnimeService = async () => {
      const knimeServiceToInit = new IFrameKnimeService();
      await knimeServiceToInit.waitForInitialization();
      return knimeServiceToInit;
    };
    this._knimeService = _createKnimeService();

    const _createJsonDataService = async () => {
      const resolvedKnimeService = await this._knimeService;
      const jsonDataService = new JsonDataService(resolvedKnimeService);
      jsonDataService.registerDataGetter(() => this._settings);
      return jsonDataService;
    };
    this._jsonDataService = _createJsonDataService();

    // Start the event poller
    this.eventPoller();
  }

  private async eventPoller() {
    // TODO(AP-19341) use Java-to-JS events

    // eslint-disable-next-line no-constant-condition
    while (this._runEventPoller) {
      // Get the next event
      const res = await this.sendToService("getEvent");
      // Give the event to the handler
      if (res) {
        if (res.type in this._eventHandlers) {
          this._eventHandlers[res.type](res.data);
        } else {
          throw new Error(
            `Got unexpected event from Java with type ${res.type}`,
          );
        }
      }
      /* else: no event wait for the next one */
    }
  }

  public stopEventPoller() {
    this._runEventPoller = false;
  }

  public async sendToService(
    methodName: string,
    options?: any[],
  ): Promise<any> {
    return (await this._jsonDataService).data({
      method: methodName,
      options,
    });
  }

  public async getInitialSettings(): Promise<NodeSettings> {
    return (await this._jsonDataService).initialData();
  }

  public async saveSettings(settings: NodeSettings): Promise<void> {
    this._settings = settings;
    (await this._jsonDataService).applyData();
  }

  public async closeDialog() {
    (await this._knimeService).closeWindow();
  }

  public registerEventHandler(type: string, handler: (args: any) => void) {
    this._eventHandlers[type] = handler;
  }

  public registerLanguageServerEventHandler(
    handler: (message: string) => void,
  ) {
    this.registerEventHandler("language-server", handler);
  }

  public registerConsoleEventHandler(handler: (text: ConsoleText) => void) {
    this.registerEventHandler("console", handler);
  }

  public sendToConsole(text: ConsoleText) {
    const consoleEventHandler = this._eventHandlers?.console;
    if (typeof consoleEventHandler === "undefined") {
      throw Error("Console handler has not yet been registered");
    }
    // Get the key-value pairs of the ConsoleText object
    const entries = Object.entries(text);

    // Get the first entry (since ConsoleText is a XOR type)
    const key = entries[0][0] as "warning" | "error" | "text";
    const value = entries[0][1];

    // Check if the value doesn't end with '\n' and append it if it doesn't
    if (!value.endsWith("\n")) {
      text[key] += "\n";
    }
    consoleEventHandler(text);
  }

  public initEditorService(
    editor: monaco.IStandaloneCodeEditor,
    editorModel: monaco.ITextModel,
  ) {
    this._editorService.initEditorService({ editor, editorModel });
  }

  public getScript(): string | null {
    return this._editorService.getScript();
  }

  public getSelectedLines(): string | null {
    return this._editorService.getSelectedLines();
  }

  public async connectToLanguageServer(): Promise<void> {
    if (typeof this._editorService.editorModel === "undefined") {
      throw Error("Editor model has not yet been initialized");
    }
    const status = (await this.sendToService(
      "connectToLanguageServer",
    )) as LanugageServerStatus;
    if (status.status === "RUNNING") {
      this._monacoLSPConnection = await MonacoLSPConnection.create(
        this._editorService.editorModel,
        new KnimeMessageReader(),
        new KnimeMessageWriter(),
      );
    } else {
      this.sendToConsole({
        text: status.message ?? "Starting the language server failed",
      });
    }
  }

  public async configureLanguageServer(settings: any): Promise<void> {
    if (this._monacoLSPConnection) {
      await this._monacoLSPConnection.changeConfiguration(settings);
    }
  }

  public setScript(newScript: string) {
    this._editorService.setScript(newScript);
  }

  public pasteToEditor(textToPaste: string): void {
    this._editorService.pasteToEditor(textToPaste);
  }

  public setOnDidChangeContentListener(callback: Function): IDisposable | null {
    return this._editorService.setOnDidChangeContentListener(callback);
  }

  public isCodeAssistantEnabled(): Promise<boolean> {
    return this.sendToService("isCodeAssistantEnabled");
  }

  public isCodeAssistantInstalled(): Promise<boolean> {
    return this.sendToService("isCodeAssistantInstalled");
  }

  public inputsAvailable(): Promise<boolean> {
    return this.sendToService("inputsAvailable");
  }

  public getFlowVariableInputs(): Promise<InputOutputModel> {
    return this.sendToService("getFlowVariableInputs");
  }

  public getInputObjects(): Promise<InputOutputModel[]> {
    return this.sendToService("getInputObjects");
  }

  public getOutputObjects(): Promise<InputOutputModel[]> {
    return this.sendToService("getOutputObjects");
  }

  // eslint-disable-next-line class-methods-use-this
  public clearConsole() {
    this._clearConsoleCallback?.();
  }
}

// only for internal use inside knime-scripting-editor
export class ScriptingServiceInternal extends ScriptingService {
  public initClearConsoleCallback(clearConsoleCallback: () => void) {
    this._clearConsoleCallback = clearConsoleCallback;
  }
}

export type ScriptingServiceType = Pick<
  ScriptingService,
  keyof ScriptingService
>;

let activeScriptingAPI: ScriptingServiceInternal;

const getScriptingService = (
  mock?: Partial<ScriptingServiceType>,
): ScriptingServiceInternal => {
  if (typeof activeScriptingAPI === "undefined") {
    const scriptingService = new ScriptingServiceInternal();
    if (typeof mock === "undefined") {
      activeScriptingAPI = scriptingService;
    } else {
      scriptingService.stopEventPoller();
      activeScriptingAPI = Object.assign(scriptingService, mock);
    }
  }
  return activeScriptingAPI;
};

export { getScriptingService };
