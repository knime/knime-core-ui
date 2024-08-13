import { sleep } from "@knime/utils";
import type { InputOutputModel } from "./components/InputOutputItem.vue";
import type { NodeSettings, ScriptingServiceType } from "./scripting-service";

const DEFAULT_PORT_CONFIGS = {
  inputPorts: [
    {
      nodeId: "root",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
    {
      nodeId: "notRoot",
      portName: "firstPort",
      portIdx: 1,
      portViewConfigs: [
        { portViewIdx: 0, label: "firstView" },
        { portViewIdx: 1, label: "secondView" },
      ],
    },
  ],
};
const SLEEP_TIME_ANY_CALL = 100;
const SLEEP_TIME_AI_SUGGESTION = 2000;

const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};

const error = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.error(message, ...args);
  } else {
    consola.error(message, ...args);
  }
};

export const DEFAULT_INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    portType: "table",
    subItems: [
      {
        name: "Column 1",
        type: "Number",
        supported: true,
      },
      {
        name: "Column 2",
        type: "String",
        supported: true,
      },
      {
        name: "Column 3",
        type: "Bit Vector",
        supported: false,
      },
    ],
  },
];

export const DEFAULT_OUTPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Output table 1",
    portType: "table",
  },
];

export const DEFAULT_FLOW_VARIABLE_INPUTS: InputOutputModel = {
  name: "Flow Variables",
  portType: "flowVariable",
  subItems: [
    {
      name: "flowVar1",
      type: "Number",
      supported: true,
    },
    {
      name: "flowVar2",
      type: "String",
      supported: true,
    },
    {
      name: "flowVar3",
      type: "Something",
      supported: false,
    },
  ],
};

export type ScriptingServiceMockOptions = {
  sendToServiceMockResponses?: Record<
    string,
    (options?: any[]) => Promise<any>
  >;
  initialSettings?: NodeSettings;
  inputsAvailable?: boolean;
  inputObjects?: InputOutputModel[];
  outputObjects?: InputOutputModel[];
  flowVariableInputs?: InputOutputModel;
};

export const createScriptingServiceMock = (
  opt: ScriptingServiceMockOptions,
): ScriptingServiceType & {
  eventHandlers: Map<string, (args: any) => void>;
} => {
  const eventHandlers = new Map<string, (args: any) => void>();
  const sendToServiceMockResponses = {
    getHubId: () => Promise.resolve("My Mocked KNIME Hub"),
    isLoggedIn: () => Promise.resolve(true),
    suggestCode: async () => {
      await sleep(SLEEP_TIME_AI_SUGGESTION);
      const fn = eventHandlers.get("codeSuggestion");
      if (typeof fn !== "undefined") {
        fn({
          status: "SUCCESS",
          code: JSON.stringify({ code: "// THIS IS A FAKE AI SUGGESTION" }),
        });
      }
      return {};
    },
    abortSuggestCodeRequest: () => Promise.resolve(),
  } as Record<string, (options?: any[]) => Promise<any>>;

  return {
    async sendToService(methodName: string, options?: any[]) {
      log(`Called scriptingService.sendToService("${methodName}")`, options);
      await sleep(SLEEP_TIME_ANY_CALL);
      if (
        opt.sendToServiceMockResponses &&
        methodName in opt.sendToServiceMockResponses
      ) {
        return opt.sendToServiceMockResponses[methodName](options);
      }

      if (methodName in sendToServiceMockResponses) {
        return sendToServiceMockResponses[methodName](options);
      }

      // Fallback - Log an error and return undefined
      error(
        `${methodName} not implemented in sendToServiceMockResponses.
      Returning undefined.`,
      );
      return Promise.resolve();
    },

    // Settings and dialog window
    getInitialSettings() {
      log("Called scriptingService.getInitialSettings");
      return Promise.resolve(opt.initialSettings ?? { script: "Hello world" });
    },
    registerSettingsGetterForApply() {
      log("Called scriptingService.registerSettingsGetterForApply");
      return Promise.resolve();
    },

    // Input and output objects
    inputsAvailable() {
      log("Called scriptingService.inputsAvailable");
      return Promise.resolve(opt.inputsAvailable ?? true);
    },
    getInputObjects() {
      log("Called scriptingService.getInputObjects");
      return Promise.resolve(opt.inputObjects ?? DEFAULT_INPUT_OBJECTS);
    },
    getOutputObjects() {
      log("Called scriptingService.getOutputObjects");
      return Promise.resolve(opt.outputObjects ?? DEFAULT_OUTPUT_OBJECTS);
    },
    getFlowVariableInputs() {
      log("Called scriptingService.getFlowVariableInputs");
      return Promise.resolve(
        opt.flowVariableInputs ?? DEFAULT_FLOW_VARIABLE_INPUTS,
      );
    },
    getPortConfigs() {
      log("Called scriptingService.getPortConfigs");
      return Promise.resolve(DEFAULT_PORT_CONFIGS);
    },
    isCallKnimeUiApiAvailable() {
      log("Called scriptingService.isCallKnimeUiApiAvailable");
      return Promise.resolve(true);
    },

    // Code assistant
    isCodeAssistantInstalled() {
      log("Called scriptingService.isCodeAssistantInstalled");
      return Promise.resolve(true);
    },
    isCodeAssistantEnabled() {
      log("Called scriptingService.isCodeAssistantEnabled");
      return Promise.resolve(true);
    },

    // Event handler
    registerEventHandler(type, handler) {
      log("Called scriptingService.registerEventHandler", type);
      eventHandlers.set(type, handler);
    },

    // Language server
    connectToLanguageServer() {
      log("Called scriptingService.connectToLanguageServer");
      return Promise.reject(new Error("No language server in mock"));
    },

    // Console handling
    eventHandlers,
  };
};
