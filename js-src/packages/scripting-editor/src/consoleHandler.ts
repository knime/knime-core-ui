import type { ConsoleHandler } from "@s/components/OutputConsole.vue";

export let consoleHandler = {
  writeln: (text) => consola.error("Console not yet initialized", text),
  write: (text) => consola.error("Console not yet initialized", text),
  clear: () => consola.error("Console not yet initialized"),
} as ConsoleHandler;

export const setConsoleHandler = (handler: ConsoleHandler) => {
  consoleHandler = handler;
};
