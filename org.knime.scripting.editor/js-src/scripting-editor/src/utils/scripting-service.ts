import { DialogService, IFrameKnimeService, JsonDataService } from 'knime-ui-extension-service';
import { FlowVariableSetting } from 'knime-ui-extension-service/src/types/FlowVariableSettings';
import { MonacoLanguageClient } from 'monaco-languageclient';
import { DocumentSelector } from 'vscode-languageserver-protocol';
import { startKnimeLanguageClient } from './knime-lsp';

export const muteReactivity = (target: object, nonReactiveKeys?: string[], reactiveKeys: string[] = []) => {
    try {
        (nonReactiveKeys || Object.keys(target))
            .filter((key: string) => !reactiveKeys.includes(key))
            .forEach((key: string) => {
                Object.defineProperty(target, key, {
                    configurable: false,
                    writable: true
                });
            });
    } catch (e) {
        // Do nothing.
    }
};

export interface ConsoleText {
    text: string;
    stderr: boolean;
}

export interface NodeSettings {
    script: string;
}

export class ScriptingService<T extends NodeSettings> {
    private readonly jsonDataService: JsonDataService;
    protected readonly flowVariableSettings: {[key: string]: FlowVariableSetting}; // TODO(UIEXT-479) refactor how flow variable information are provided
    protected readonly initialNodeSettings: T;
    protected currentNodeSettings: T;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    private eventHandlers: { [type: string]: (args: any) => void };

    constructor({
        jsonDataService,
        flowVariableSettings,
        initialNodeSettings
    }: {
        jsonDataService: JsonDataService;
        flowVariableSettings: {[key: string]: FlowVariableSetting};
        initialNodeSettings: T;
    }) {
        this.jsonDataService = jsonDataService;
        this.flowVariableSettings = flowVariableSettings;
        this.initialNodeSettings = initialNodeSettings;
        this.eventHandlers = {};
        this.currentNodeSettings = JSON.parse(JSON.stringify(initialNodeSettings));

        this.jsonDataService.registerDataGetter(() => this.currentNodeSettings);

        // Start the event poller loop
        this.eventPoller();
    }

    private async eventPoller() {
        // TODO(AP-19340) use long-polling
        // TODO(AP-19341) use Java-to-JS events

        // eslint-disable-next-line no-constant-condition
        while (true) {
            // Get the next event
            const res = await this.sendToService('getEvent');

            // Give the event to the handler
            if (res) {
                if (res.type in this.eventHandlers) {
                    this.eventHandlers[res.type](res.data);
                } else {
                    throw new Error(`Got unexpected event from Java with type ${res.type}`);
                }
            } else {
                // Empty response wait for the next poll
                await new Promise((r) => setTimeout(r, 100));
            }
        }
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    protected sendToService(methodName: string, options?: any[]): Promise<any> {
        return this.jsonDataService.data({
            method: `ScriptingService.${methodName}`,
            options
        });
    }

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    protected registerEventHandler(type: string, handler: (args: any) => void) {
        this.eventHandlers[type] = handler;
    }

    // Settings handling

    getInitialScript() {
        return this.initialNodeSettings.script;
    }

    getScript() {
        return this.currentNodeSettings.script;
    }

    setScript(script: string) {
        this.currentNodeSettings.script = script;
    }

    applySettings() {
        return this.jsonDataService.applyData();
    }

    // TODO is this implemented in the backend?
    async applySettingsAndExecute() {
        await this.applySettings();
        return this.sendToService('execute');
    }

    startLanguageClient(name: string, documentSelector?: DocumentSelector | string[]): MonacoLanguageClient {
        return startKnimeLanguageClient(this, name, documentSelector);
    }

    sendLanguageServerMessage(message: string) {
        return this.jsonDataService.data({
            method: 'ScriptingService.sendLanguageServerMessage',
            options: [message]
        });
    }

    registerLanguageServerEventHandler(handler: (message: string) => void) {
        this.registerEventHandler('language-server', handler);
    }

    registerConsoleEventHandler(handler: (text: ConsoleText) => void) {
        this.registerEventHandler('console', handler);
    }
}

export const createJsonServiceAndLoadSettings = async () => {
    const knimeService = new IFrameKnimeService();
    await knimeService.waitForInitialization();
    const jsonDataService = new JsonDataService(knimeService);
    const dialogService = new DialogService(knimeService);
    return {
        jsonDataService,
        flowVariableSettings: await dialogService.getFlowVariableSettings(),
        initialNodeSettings: await jsonDataService.initialData()
    };
};

/**
 * Create a ScriptingService and mute its reactivity for Vue.
 * @returns {Promise<ScriptingService<NodeSettings>>} a new scripting service that is ready to be used
 */
export const createScriptingService = async (): Promise<ScriptingService<NodeSettings>> => {
    const scriptingService = new ScriptingService(await createJsonServiceAndLoadSettings());
    muteReactivity(scriptingService);
    return scriptingService;
};
