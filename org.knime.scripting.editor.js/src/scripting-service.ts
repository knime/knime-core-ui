import { JsonDataService, DialogService } from "@knime/ui-extension-service";
import type { InputOutputModel } from "./components/InputOutputItem.vue";
import { useMainCodeEditorStore } from "./editor";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import { consoleHandler } from "@/consoleHandler";
import {
  getScriptingServiceInstance,
  setScriptingServiceInstance,
} from "./scripting-service-instance";

export type NodeSettings = { script: string; scriptUsedFlowVariable?: string };
type LanugageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

class ScriptingService {
  private _jsonDataService: Promise<JsonDataService>;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};
  private _runEventPoller: boolean = true;
  private _monacoLSPConnection: MonacoLSPConnection | null = null;

  constructor() {
    this._jsonDataService = JsonDataService.getInstance();

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

  // NB: this is only used in tests
  private stopEventPoller() {
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

  public registerEventHandler(type: string, handler: (args: any) => void) {
    this._eventHandlers[type] = handler;
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

/**
 * Get the singleton instance of the scripting service. The scripting service
 * provides access to the backend KNIME instance.
 *
 * Note that it is possible to provide a custom scripting service instance in
 * a browser development environment (see example).
 *
 * @returns the scripting service instance
 * @example
 * // Provide a custom scripting service instance in a browser development environment
 * // vite.config.ts
 *   resolve: {
 *   alias: {
 *     "./scripting-service-instance.js":
 *       process.env.APP_ENV === "browser"
 *         ? <path to scripting-service-instance-browser.ts>,
 *         : "./scripting-service-instance.js",
 *   },
 * },
 *
 * // scripting-service-instance-browser.ts
 * import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
 * const scriptingService = createScriptingServiceMock({
 *   // optional customizations
 * });
 * export const getScriptingServiceInstance = () => scriptingService;
 * export const setScriptingServiceInstance = () => {};
 */
export const getScriptingService = (): ScriptingServiceType =>
  getScriptingServiceInstance();

export const initConsoleEventHandler = () => {
  getScriptingService().registerEventHandler("console", consoleHandler.write);
};

export const registerSettingsGetterForApply = async (
  settingsGetter: () => NodeSettings,
) => {
  const dialogService = await DialogService.getInstance();
  const jsonDataService = await JsonDataService.getInstance();

  dialogService.setApplyListener(async () => {
    const settings = settingsGetter();
    await jsonDataService.applyData(settings);
    return { isApplied: true };
  });
};

setScriptingServiceInstance(new ScriptingService());
