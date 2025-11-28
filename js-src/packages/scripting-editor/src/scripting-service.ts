/* eslint-disable @typescript-eslint/no-explicit-any */
import type {
  AlertParams,
  AlertingService,
  JsonDataService,
} from "@knime/ui-extension-service";

import { useMainCodeEditorStore } from "./editor";
import type { PortConfig } from "./initial-data-service";
import { MonacoLSPConnection } from "./lsp/connection";
import { KnimeMessageReader, KnimeMessageWriter } from "./lsp/knime-io";
import type { PublicAPI } from "./types/public-api";

type LanguageServerStatus = { status: "RUNNING" | "ERROR"; message?: string };

type UnknownUsageData = { type: "UNKNOWN" };
type LimitedUsageData = { type: "LIMITED"; limit: number; used: number };
type UnlimitedUsageData = { type: "UNLIMITED" };
type UnlicensedUsageData = { type: "UNLICENSED"; message: string };

/**
 * Usage data for K-AI interactions.
 *
 * This type must match the JSON serialization of the Java type
 * `org.knime.core.webui.node.dialog.scripting.kai.CodeKaiHandler.KaiUsage`.
 */
export type UsageData =
  | UnknownUsageData
  | LimitedUsageData
  | UnlimitedUsageData
  | UnlicensedUsageData;

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
   * Internal constructor — use `getScriptingService()` from `init.ts` instead.
   *
   * Client applications will call `init()` first, then use the getter to access
   * the service. Within this repository, developers can assume `init()` has been
   * called and the scripting service is available via `getScriptingService()`.
   *
   * This constructor should only be called from the `init()` function and in unit tests.
   *
   * @internal
   * @param jsonDataService The JSON data service used for communication with the backend.
   * @param alertingService The alerting service used to send alerts.
   */
  constructor(
    private readonly jsonDataService: JsonDataService,
    private readonly alertingService: AlertingService,
  ) {
    this.eventPoller = new EventPoller(this.jsonDataService);
  }

  sendToService(methodName: string, options?: any[] | undefined): Promise<any> {
    return this.jsonDataService.data({
      method: `ScriptingService.${methodName}`,
      options,
    });
  }

  getOutputPreviewTableInitialData(): Promise<string | undefined> {
    return this.jsonDataService.data({
      method: "OutputPreviewTableInitialDataRpcSupplier.getInitialData",
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
        new KnimeMessageReader((handler) =>
          this.registerEventHandler("language-server", handler),
        ),
        new KnimeMessageWriter((message) =>
          this.sendToService("sendLanguageServerMessage", [message]),
        ),
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

  getAiUsage(): Promise<UsageData> {
    return this.sendToService("getAiUsage");
  }

  sendAlert(alert: AlertParams) {
    this.alertingService?.sendAlert(alert);
  }
}

/** Type representing the public API of ScriptingService */
export type ScriptingServiceType = PublicAPI<ScriptingService>;
