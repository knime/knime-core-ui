import {
  IFrameKnimeService,
  JsonDataService,
} from "@knime/ui-extension-service";
import type { InputOutputModel } from "./components/InputOutputItem.vue";
import type { ConsoleText } from "./components/OutputConsole.vue";
import { useMainCodeEditorStore } from "./editor";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import { consoleHandler } from "@/consoleHandler";

export type NodeSettings = { script: string; scriptUsedFlowVariable?: string };
type LanugageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

class ScriptingService {
  private _knimeService: Promise<IFrameKnimeService>;
  private _jsonDataService: Promise<JsonDataService>;
  private _settings: NodeSettings | null = null;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};
  private _runEventPoller: boolean = true;
  private _monacoLSPConnection: MonacoLSPConnection | null = null;

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

  public async connectToLanguageServer(): Promise<void> {
    // TODO move the complete logic somewhere else?
    const editorModel = useMainCodeEditorStore().value?.editorModel;
    if (typeof editorModel === "undefined") {
      throw Error("Editor model has not yet been initialized");
    }
    const status = (await this.sendToService(
      "connectToLanguageServer",
    )) as LanugageServerStatus;
    if (status.status === "RUNNING") {
      this._monacoLSPConnection = await MonacoLSPConnection.create(
        editorModel,
        new KnimeMessageReader(),
        new KnimeMessageWriter(),
      );
    } else {
      consoleHandler.writeln({
        text: status.message ?? "Starting the language server failed",
      });
    }
  }

  public async configureLanguageServer(settings: any): Promise<void> {
    if (this._monacoLSPConnection) {
      await this._monacoLSPConnection.changeConfiguration(settings);
    }
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
}

export type ScriptingServiceType = Pick<
  ScriptingService,
  keyof ScriptingService
>;

let activeScriptingAPI: ScriptingServiceType;

const getScriptingService = (
  mock?: ScriptingServiceType,
): ScriptingServiceType => {
  if (typeof activeScriptingAPI === "undefined") {
    if (typeof mock === "undefined") {
      activeScriptingAPI = new ScriptingService();
    } else {
      activeScriptingAPI = mock;
    }
  }
  return activeScriptingAPI;
};

export { getScriptingService };
