/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-magic-numbers */
import { sleep } from "@knime/utils";

import { log } from "@s/log";

import type { ScriptingServiceType } from "./scripting-service";

const SLEEP_TIME_ANY_CALL = 100;
const SLEEP_TIME_AI_SUGGESTION = 2000;

const error = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.error(message, ...args);
  } else {
    consola.error(message, ...args);
  }
};

export type ScriptingServiceMockOptions = {
  sendToServiceMockResponses?: Record<
    string,
    (...options: any) => Promise<any>
  >;
};

export const createScriptingServiceMock = (
  opt: ScriptingServiceMockOptions,
): ScriptingServiceType & {
  eventHandlers: Map<string, (args: any) => void>;
} => {
  let aiUsage = 498;
  const eventHandlers = new Map<string, (args: any) => void>();
  const sendToServiceMockResponses: Record<
    string,
    (options?: any[]) => Promise<any>
  > = {
    suggestCode: async () => {
      await sleep(SLEEP_TIME_AI_SUGGESTION);
      const fn = eventHandlers.get("codeSuggestion");
      if (typeof fn !== "undefined") {
        fn({
          status: "SUCCESS",
          code: JSON.stringify({
            code: "// THIS IS A FAKE AI SUGGESTION",
            interactionId: "mock-interaction-id",
            usage: {
              limit: 500,
              used: aiUsage++,
            },
          }),
        });
      }
      return {};
    },
    abortSuggestCodeRequest: () => Promise.resolve(),
  };

  return {
    async sendToService(methodName: string, options?: any[]) {
      log(`Called scriptingService.sendToService("${methodName}")`, options);

      await sleep(SLEEP_TIME_ANY_CALL);

      if (
        opt.sendToServiceMockResponses &&
        methodName in opt.sendToServiceMockResponses
      ) {
        return opt.sendToServiceMockResponses[methodName](...(options ?? []));
      }

      if (methodName in sendToServiceMockResponses) {
        return sendToServiceMockResponses[methodName](...(options ?? []));
      }

      // Fallback - Log an error and return undefined
      error(
        `${methodName} not implemented in sendToServiceMockResponses.
      Returning undefined.`,
      );
      return Promise.resolve();
    },

    getOutputPreviewTableInitialData() {
      return Promise.resolve(undefined);
    },

    // Settings and dialog window
    isCallKnimeUiApiAvailable() {
      log("Called scriptingService.isCallKnimeUiApiAvailable");
      return Promise.resolve(false);
    },

    isKaiEnabled() {
      log("Called scriptingService.isKaiEnabled");
      return Promise.resolve(true);
    },

    isLoggedIntoHub() {
      log("Called scriptingService.isLoggedIntoHub");
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

    getAiDisclaimer() {
      log("Called scriptingService.getAiDisclaimer");
      return Promise.resolve(
        "This is a fake AI disclaimer. Enjoy the AI. It does nothing!",
      );
    },

    getAiUsage() {
      log("Called scriptingService.getAiUsage");
      return Promise.resolve({
        limit: 500,
        used: aiUsage,
      });
    },

    sendAlert(alert) {
      log("Called scriptingService.sendAlert", alert);
    },

    // Console handling
    eventHandlers,
  };
};
