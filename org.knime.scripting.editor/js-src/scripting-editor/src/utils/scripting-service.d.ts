import { JsonDataService } from '@knime/ui-extension-service';
import type { FlowVariableSetting } from '@knime/ui-extension-service';
import type { MonacoLanguageClient } from 'monaco-languageclient';
import type { DocumentSelector } from 'vscode-languageserver-protocol';
export declare const muteReactivity: (
  target: object,
  nonReactiveKeys?: string[] | undefined,
  reactiveKeys?: string[]
) => void;
export interface ConsoleText {
  text: string;
  stderr: boolean;
}
export interface NodeSettings {
  script: string;
}
export declare class ScriptingService<T extends NodeSettings> {
    private readonly jsonDataService;
    protected readonly flowVariableSettings: {
    [key: string]: FlowVariableSetting;
  };

    protected readonly initialNodeSettings: T;
    protected currentNodeSettings: T;
    private eventHandlers;
    constructor({
        jsonDataService,
        flowVariableSettings,
        initialNodeSettings
    }: {
    jsonDataService: JsonDataService;
    flowVariableSettings: {
      [key: string]: FlowVariableSetting;
    };
    initialNodeSettings: T;
  });

    private eventPoller;
    protected sendToService(methodName: string, options?: any[]): Promise<any>;
    protected registerEventHandler(
    type: string,
    handler: (args: any) => void
  ): void;

    getInitialScript(): string;
    getScript(): string;
    setScript(script: string): void;
    applySettings(): Promise<any>;
    applySettingsAndExecute(): Promise<any>;
    startLanguageClient(
    name: string,
    documentSelector?: DocumentSelector | string[]
  ): MonacoLanguageClient;

    sendLanguageServerMessage(message: string): Promise<any>;
    registerLanguageServerEventHandler(handler: (message: string) => void): void;
    registerConsoleEventHandler(handler: (text: ConsoleText) => void): void;
}
export declare const createJsonServiceAndLoadSettings: () => Promise<{
  jsonDataService: JsonDataService<any>;
  flowVariableSettings: any;
  initialNodeSettings: any;
}>;
/**
 * Create a ScriptingService and mute its reactivity for Vue.
 * @returns {Promise<ScriptingService<NodeSettings>>} a new scripting service that is ready to be used
 */
export declare const createScriptingService: () => Promise<
  ScriptingService<NodeSettings>
>;
