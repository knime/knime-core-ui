import {
  DialogService,
  IFrameKnimeService,
  JsonDataService,
} from "@knime/ui-extension-service";

export type ConsoleText = { text: string; stderr: boolean };

export type NodeSettings = { script: string };

class ScriptingService {
  private _knimeService: Promise<IFrameKnimeService>;
  private _jsonDataService: Promise<JsonDataService>;
  private _dialogService: Promise<DialogService>;
  private _settings: NodeSettings | null = null;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};
  private _runEventPoller: boolean = true;

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

    const _createDialogService = async () => {
      const resolvedKnimeService = await this._knimeService;
      return new DialogService(resolvedKnimeService);
    };
    this._dialogService = _createDialogService();

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

  public async getFlowVariableSettings(): Promise<any> {
    // TODO: the ui-extension-service typings are "any" here but we do know something of the type
    return (await this._dialogService).getFlowVariableSettings();
  }

  public async saveSettings(settings: NodeSettings): Promise<void> {
    this._settings = settings;
    (await this._jsonDataService).applyData();
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
}

export type ScriptingServiceType = Pick<
  ScriptingService,
  keyof ScriptingService
>;

let activeScriptingAPI: ScriptingServiceType;

const getScriptingService = (mock?: ScriptingServiceType) => {
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
