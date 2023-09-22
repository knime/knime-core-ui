<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useResizeObserver, useDebounceFn } from "@vueuse/core";

import type { XOR } from "ts-xor";
import type { ITerminalOptions, ITheme, ITerminalInitOnlyOptions } from "xterm";
import { Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import { Unicode11Addon } from "xterm-addon-unicode11";
import * as knimeColors from "webapps-common/ui/colors/knimeColors.mjs";
import TrashIcon from "webapps-common/ui/assets/img/icons/trash.svg";
import Button from "webapps-common/ui/components/Button.vue";

export type ConsoleText = XOR<
  { text: string },
  { warning: string },
  { error: string }
>;

export type ConsoleHandler = (text: ConsoleText) => void;

const ANSI_ERROR_START = "\u001b[48;5;224m\u001b[30m";
const ANSI_WARNING_START = "\u001b[47m\u001b[30m";
const ANSI_RESET = "\u001b[0m";
const DEBOUNCE_TIME = 300;

const theme: ITheme = {
  background: knimeColors.White,
  foreground: knimeColors.Black,
  selectionBackground: knimeColors.CornflowerSemi,
  cursor: knimeColors.StoneGray,
  black: knimeColors.Black,
};

const options: ITerminalOptions & ITerminalInitOnlyOptions = {
  letterSpacing: 0, // in pixel, needs to be defined otherwise it's dynamic
  allowProposedApi: true,
  fontSize: 12,
  convertEol: true, // otherwise \n doesn't start at beginning of new line
  fontFamily: '"Roboto Mono", sans-serif',
  disableStdin: true,
  cursorBlink: false,
  theme,
};

const term = new Terminal(options);
const termRef = ref<HTMLInputElement | null>(null);

const unicode11Addon = new Unicode11Addon();
term.loadAddon(unicode11Addon);
term.unicode.activeVersion = "11";

const fitAddon = new FitAddon();
term.loadAddon(fitAddon);
const updateConsole = useDebounceFn(() => {
  fitAddon.fit();
}, DEBOUNCE_TIME);
useResizeObserver(termRef, () => {
  updateConsole();
});

type ConsoleHandlerEmit = (
  e: "console-created",
  handler: ConsoleHandler,
) => void;

const emit = defineEmits<ConsoleHandlerEmit>();

const write = (text: ConsoleText) => {
  if ("error" in text) {
    term.write(`❌ ${ANSI_ERROR_START}${text.error}${ANSI_RESET}`);
  } else if ("warning" in text) {
    term.write(`⚠️  ${ANSI_WARNING_START}${text.warning}${ANSI_RESET}`);
  } else {
    term.write(text.text);
  }
};

onMounted(() => {
  term.open(termRef.value as HTMLElement);
  emit("console-created", write);
  const listener = term.onLineFeed(() => {
    fitAddon.fit();
    listener.dispose();
  });
});

onUnmounted(() => {
  term?.dispose();
});
</script>

<template>
  <Button class="clear-button" @click="term.reset()">
    <TrashIcon />
  </Button>
  <div ref="termRef" class="terminal" />
</template>

<style lang="postcss">
@import url("xterm/css/xterm.css");
</style>

<style lang="postcss" scoped>
.clear-button {
  position: absolute;
  z-index: 1;
  top: -50px;
  right: 0;
}

.terminal {
  height: 100%;
}
</style>
