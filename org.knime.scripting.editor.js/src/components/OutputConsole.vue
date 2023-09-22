<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useResizeObserver, useDebounceFn } from "@vueuse/core";

import type { XOR } from "ts-xor";
import type { ITerminalOptions, ITheme, ITerminalInitOnlyOptions } from "xterm";
import { Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import * as knimeColors from "webapps-common/ui/colors/knimeColors.mjs";

import Button from "webapps-common/ui/components/Button.vue";

export type ConsoleText = XOR<
  { text: string },
  { warning: string },
  { error: string }
>;

export type ConsoleHandler = (text: ConsoleText) => void;

const RED = "\x1b[31m";
const YELLOW = "\x1b[33m";
const BOLD = "\x1b[1m";
const RESET = "\x1b[0m";
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
  fontSize: 10,
  convertEol: true, // otherwise \n doesn't start at beginning of new line
  fontFamily: '"Roboto Mono", sans-serif',
  disableStdin: true,
  cursorBlink: false,
  theme,
};

const term = new Terminal(options);
const termRef = ref<HTMLInputElement | null>(null);
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
    term.write(`${RED}${BOLD}${text.error}${RESET}`);
  } else if ("warning" in text) {
    term.write(`${YELLOW}${text.warning}${RESET}`);
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
  <div ref="termRef" class="terminal" />
  <Button class="clear-button" compact @click="term.reset()">clear </Button>
</template>

<style lang="postcss">
@import url("xterm/css/xterm.css");

.terminal {
  position: relative;
  width: 100%;
  height: calc(100% - 20px);
  padding-top: 5px;
  padding-left: 5px;

  & .xterm {
    height: 100%;
  }
}

.clear-button {
  stroke-width: 5px;
  background-color: transparent;
  margin-top: 0;
  margin-bottom: 5px;
  position: relative;
  bottom: 5px;
  float: right;

  /* best way to ensure pill shaped buttons with flexible 1/4 corners */
  border-radius: var(--theme-button-border-radius, 9999px);
}
</style>
