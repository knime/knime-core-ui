/* eslint-disable @typescript-eslint/no-explicit-any */
import type { JsonDataService } from "@knime/ui-extension-service";

import { consoleHandler } from "./consoleHandler";
import { useMainCodeEditorStore } from "./editor";
import { getScriptingService } from "./init";
import type { PortConfig } from "./initial-data-service";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import type { PublicAPI } from "./types/public-api";

type LanguageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

export type UsageData = {
  limit: number | null;
  used: number;
};

// TODO AP-19341: use Java-to-JS events
/**
 * This class is a singleton that polls for events from the
 * Java backend and calls event handlers.
 */
class EventPoller {
  private _eventHandlers: { [type: string]: (args: any) => void } = {};

  constructor(private readonly jsonDataService: JsonDataService) {
    this.startPolling().catch(() => {});
  }

  private async startPolling() {
    while (true) {
      const res = await this.jsonDataService.data<{ type: string; data: any }>({
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
}

export class ScriptingService {
  private readonly eventPoller: EventPoller;

  /**
   * Internal constructor — do not call directly.
   *
   * This constructor is intended for internal use only.
   * Please use the `init` method in `init.ts` to create the singleton instance and
   * access via `getScriptingService()`. Only one instance should be created via the
   * designated initialization method.
   *
   * @internal
   * @param jsonDataService The service used for JSON data operations.
   */
  constructor(private readonly jsonDataService: JsonDataService) {
    this.eventPoller = new EventPoller(this.jsonDataService);
  }

  sendToService(methodName: string, options?: any[] | undefined): Promise<any> {
    return this.jsonDataService.data({
      method: `ScriptingService.${methodName}`,
      options,
    });
  }

  registerEventHandler(type: string, handler: (args: any) => void) {
    this.eventPoller.registerEventHandler(type, handler);
  }

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
  }

  // Parameters need to be a valid port config, otherwise the call will fail
  // even when callKnimeUiApi is available
  async isCallKnimeUiApiAvailable(portToTestFor: PortConfig): Promise<boolean> {
    const baseService = (this.jsonDataService as any).baseService;
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

  isKaiEnabled(): Promise<boolean> {
    return this.sendToService("isKaiEnabled");
  }

  isLoggedIntoHub(): Promise<boolean> {
    return this.sendToService("isLoggedIntoHub");
  }

  getAiDisclaimer(): Promise<string> {
    return this.sendToService("getAiDisclaimer");
  }

  getAiUsage(): Promise<UsageData | null> {
    return this.sendToService("getAiUsage");
  }
}

/** Type representing the public API of ScriptingService */
export type ScriptingServiceType = PublicAPI<ScriptingService>;

// TODO move?
export const initConsoleEventHandler = () => {
  getScriptingService().registerEventHandler("console", consoleHandler.write);
};
