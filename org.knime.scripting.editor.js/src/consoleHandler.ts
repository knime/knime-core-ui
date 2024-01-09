import type { ConsoleHandler } from "@/components/OutputConsole.vue";
import { shallowRef } from "vue";

export const consoleHandlerStore = shallowRef<ConsoleHandler>({
  writeln: (text) => consola.error("Console not yet initialized", text),
  write: (text) => consola.error("Console not yet initialized", text),
  clear: () => consola.error("Console not yet initialized"),
});

export const consoleHandler = {
  writeln: (text) => consoleHandlerStore.value.writeln(text),
  write: (text) => consoleHandlerStore.value.write(text),
  clear: () => consoleHandlerStore.value.clear(),
} as ConsoleHandler;
