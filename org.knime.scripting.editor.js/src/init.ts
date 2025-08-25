/* eslint-disable @typescript-eslint/no-explicit-any */
import { JsonDataService } from "@knime/ui-extension-service";

import { useMainCodeEditorStore } from "./editor";
import type { PortConfig } from "./initial-data-service";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";

// --- TYPES ---

type LanguageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

export type UsageData = {
  limit: number | null;
  used: number;
};

export type ScriptingServiceType = {
  sendToService(methodName: string, options?: any[] | undefined): Promise<any>;
  registerEventHandler(type: string, handler: (args: any) => void): void;
  connectToLanguageServer(): Promise<MonacoLSPConnection>;
  isCallKnimeUiApiAvailable(portToTestFor: PortConfig): Promise<boolean>;
  isKaiEnabled(): Promise<boolean>;
  isLoggedIntoHub(): Promise<boolean>;
  getAiDisclaimer(): Promise<string>;
  getAiUsage(): Promise<UsageData | null>;
};

// --- HELPER CLASSES ---

///// MOVED FROM scripting-service.ts

// TODO AP-19341: use Java-to-JS events
/**
 * This class is a singleton that polls for events from the
 * Java backend and calls event handlers.
 */
// TODO get rid of the singleton pattern and only create one instance
class EventPoller {
  private static instance: EventPoller;
  private _eventHandlers: { [type: string]: (args: any) => void } = {};

  private constructor() {
    this.startPolling().catch(() => {});
  }

  private async startPolling() {
    const jsonDataService = await JsonDataService.getInstance();

    while (true) {
      const res = await jsonDataService.data<{ type: string; data: any }>({
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

// TODO get rid of the singleton pattern and only create one instance
class RPCHelper {
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

// --- INSTANCES ---

// TODO define a class that also has the implementation and is initialized in init
export let scriptingService: ScriptingServiceType;

// --- INIT FUNCTION ---

// TODO jsdoc
export const init = async () => {
  // TODO move implementation into class (so this init method gets shorter)
  scriptingService = {
    sendToService(
      methodName: string,
      options?: any[] | undefined,
    ): Promise<any> {
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
      )) as LanguageServerStatus;
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

    getAiUsage(): Promise<UsageData | null> {
      return this.sendToService("getAiUsage");
    },
  };
};
