import { JsonDataService } from "@knime/ui-extension-service";

import { consoleHandler } from "@/consoleHandler";

import { useMainCodeEditorStore } from "./editor";
import type { PortConfig } from "./initial-data-service";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";

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

  isCallKnimeUiApiAvailable(portToTestFor: PortConfig) {
    return RPCHelper.getInstance().isCallKnimeUiApiAvailable(portToTestFor);
  },

  isKaiEnabled(): Promise<boolean> {
    return this.sendToService("isKaiEnabled");
  },

  isLoggedIntoHub(): Promise<boolean> {
    return this.sendToService("isLoggedIntoHub");
  },

  getAiDisclaimer(): Promise<string> {
    return this.sendToService("getAiDisclaimer");
  },
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
