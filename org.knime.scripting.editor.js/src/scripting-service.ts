import { consoleHandler } from "./consoleHandler";
import { type ScriptingServiceType, type UsageData, scriptingService } from "./init";

export type { ScriptingServiceType, UsageData };

// TODO move this to `init.ts` but move the implementation here
//   init.ts should handle the application state
//   scripting-service should be implemented here (but only initialized from `init.ts`)
export const getScriptingService = (): ScriptingServiceType => scriptingService;

export const initConsoleEventHandler = () => {
  getScriptingService().registerEventHandler("console", consoleHandler.write);
};
