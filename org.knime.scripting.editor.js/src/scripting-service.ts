import { JsonDataService, DialogService } from "@knime/ui-extension-service";
import type { InputOutputModel } from "./components/InputOutputItem.vue";
import { useMainCodeEditorStore } from "./editor";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import { consoleHandler } from "@/consoleHandler";

export type PortViewConfig = {
  label: string;
  portViewIdx: number;
};

type PortConfig = {
  /**
   * null if no node is connected to an input port
   */
  nodeId: string | null;
  portIdx: number;
  portViewConfigs: PortViewConfig[];
  portName: string;
};

export type PortConfigs = {
  inputPorts: PortConfig[];
};

export type NodeSettings = {
  script: string;
  scriptUsedFlowVariable?: string | null;
};
type LanugageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

// --- HELPER CLASSES ---

// TODO(AP-19341) use Java-to-JS events
/**
 * This class is a singleton that polls for events from the
 * Java backend and calls event handlers.
 */
class EventPoller {
  // eslint-disable-next-line no-use-before-define
  private static instance: EventPoller;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};

  private constructor() {
    this.startPolling();
  }

  private async startPolling() {
    const jsonDataService = await JsonDataService.getInstance();

    // eslint-disable-next-line no-constant-condition
    while (true) {
      const res = await jsonDataService.data({
        method: "ScriptingService.getEvent",
      });
      if (res) {
        if (res.type in this._eventHandlers) {
          this._eventHandlers[res.type](res.data);
        } else {
          throw new Error(
            `Got unexpected event from Java with type ${res.type}`,
          );
        }
      }
    }
  }

  public registerEventHandler(type: string, handler: (args: any) => void) {
    this._eventHandlers[type] = handler;
  }

  public static getInstance(): EventPoller {
    if (!EventPoller.instance) {
      EventPoller.instance = new EventPoller();
    }
    return EventPoller.instance;
  }
}

class RPCHelper {
  // eslint-disable-next-line no-use-before-define
  private static instance: RPCHelper;
  private jsonDataService: Promise<JsonDataService>;

  private constructor() {
    this.jsonDataService = JsonDataService.getInstance();
  }

  public async sendToService(
    methodName: string,
    options?: any[],
  ): Promise<any> {
    return (await this.jsonDataService).data({
      method: `ScriptingService.${methodName}`,
      options,
    });
  }

  // Parameters need to be a valid port config, otherwise the call will fail
  // even when callKnimeUiApi is available
  public async isCallKnimeUiApiAvailable(
    portToTestFor: PortConfig,
  ): Promise<boolean> {
    const baseService = ((await this.jsonDataService) as any).baseService;
    if (baseService === null) {
      return false;
    }

    return (
      await baseService.callKnimeUiApi!("PortService.getPortView", {
        nodeId: portToTestFor.nodeId,
        portIdx: portToTestFor.portIdx,
        viewIdx: portToTestFor.portViewConfigs[0]?.portViewIdx,
      })
    ).isSome;
  }

  public static getInstance(): RPCHelper {
    if (!RPCHelper.instance) {
      RPCHelper.instance = new RPCHelper();
    }
    return RPCHelper.instance;
  }
}

class SettingsHelper {
  // eslint-disable-next-line no-use-before-define
  private static instance: SettingsHelper;
  private jsonDataService: Promise<JsonDataService>;

  private constructor() {
    this.jsonDataService = JsonDataService.getInstance();
  }

  public async getInitialSettings(): Promise<NodeSettings> {
    return (await this.jsonDataService).initialData();
  }

  public async registerApplyListener(
    settingsGetter: () => NodeSettings,
  ): Promise<void> {
    const dialogService = await DialogService.getInstance();
    dialogService.setApplyListener(async () => {
      const settings = settingsGetter();
      try {
        await (await this.jsonDataService).applyData(settings);
        return { isApplied: true };
      } catch (e) {
        consola.warn("Failed to apply settings", e);
        return { isApplied: false };
      }
    });
  }

  public static getInstance(): SettingsHelper {
    if (!SettingsHelper.instance) {
      SettingsHelper.instance = new SettingsHelper();
    }
    return SettingsHelper.instance;
  }
}

// --- SCRIPTING SERVICE ---

const scriptingService = {
  sendToService(methodName: string, options?: any[] | undefined): Promise<any> {
    return RPCHelper.getInstance().sendToService(methodName, options);
  },
  registerEventHandler(type: string, handler: (args: any) => void) {
    EventPoller.getInstance().registerEventHandler(type, handler);
  },

  // TODO move this logic somewhere else to remove the dependency from the
  // scripting-service to the main editor state
  async connectToLanguageServer() {
    const editorModel = useMainCodeEditorStore().value?.editorModel;
    if (typeof editorModel === "undefined") {
      throw Error("Editor model has not yet been initialized");
    }
    const status = (await this.sendToService(
      "connectToLanguageServer",
    )) as LanugageServerStatus;
    if (status.status === "RUNNING") {
      return MonacoLSPConnection.create(
        editorModel,
        new KnimeMessageReader(),
        new KnimeMessageWriter(),
      );
    } else {
      throw Error(status.message ?? "Starting the language server failed");
    }
  },

  // Code assistant
  isCodeAssistantEnabled(): Promise<boolean> {
    return RPCHelper.getInstance().sendToService("isCodeAssistantEnabled");
  },
  isCodeAssistantInstalled(): Promise<boolean> {
    return RPCHelper.getInstance().sendToService("isCodeAssistantInstalled");
  },

  // Inputs and outputs
  inputsAvailable(): Promise<boolean> {
    return RPCHelper.getInstance().sendToService("inputsAvailable");
  },
  getFlowVariableInputs(): Promise<InputOutputModel> {
    return RPCHelper.getInstance().sendToService("getFlowVariableInputs");
  },
  getInputObjects(): Promise<InputOutputModel[]> {
    return RPCHelper.getInstance().sendToService("getInputObjects");
  },
  getOutputObjects(): Promise<InputOutputModel[]> {
    return RPCHelper.getInstance().sendToService("getOutputObjects");
  },
  getPortConfigs(): Promise<PortConfigs> {
    return RPCHelper.getInstance().sendToService("getInputPortConfigs");
  },
  isCallKnimeUiApiAvailable(portToTestFor: PortConfig) {
    return RPCHelper.getInstance().isCallKnimeUiApiAvailable(portToTestFor);
  },

  // Settings
  getInitialSettings: () => SettingsHelper.getInstance().getInitialSettings(),

  registerSettingsGetterForApply: (settingsGetter: () => NodeSettings) =>
    SettingsHelper.getInstance().registerApplyListener(settingsGetter),
};

export type ScriptingServiceType = typeof scriptingService;

/**
 * Get the singleton instance of the scripting service. The scripting service
 * provides access to the backend KNIME instance.
 *
 * Note that it is possible to provide a custom scripting service instance in
 * a browser development environment (see example).
 *
 * @returns the scripting service instance
 * @example
 * import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
 * import { getScriptingService } from "@knime/scripting-editor";
 *
 * if (import.meta.env.MODE === "development.browser") {
 *   const scriptingService = createScriptingServiceMock({
 *     sendToServiceMockResponses: {
 *       myBackendFunction: (options) => {
 *         consola.log("called my backend function with", options);
 *         return Promise.resolve();
 *       },
 *     },
 *   });
 *
 *   Object.assign(getScriptingService(), scriptingService);
 * }
 */
export const getScriptingService = (): ScriptingServiceType => scriptingService;

export const initConsoleEventHandler = () => {
  getScriptingService().registerEventHandler("console", consoleHandler.write);
};
