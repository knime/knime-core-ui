/**
 * This module stores the currently singleton instance of the scripting service.
 * Client code should not use this module directly but should use the
 * `getScriptingService` method instead.
 *
 * The active scripting service instance is set here to allow for a custom
 * scripting service instance in a browser development environment. See
 * `getScriptingService` for an example.
 */
import { type ScriptingServiceType } from "./scripting-service";

let activeScriptingService: ScriptingServiceType;

/**
 * Do not call this method directly! This method is only called when importing
 * the scripting service to set the singleton instance.
 * @private
 */
export const setScriptingServiceInstance = (
  scriptingService: ScriptingServiceType,
) => {
  activeScriptingService = scriptingService;
};

/**
 * Do not call this method directly! Use `getScriptingService` instead.
 * @private
 */
export const getScriptingServiceInstance = (): ScriptingServiceType =>
  activeScriptingService;
