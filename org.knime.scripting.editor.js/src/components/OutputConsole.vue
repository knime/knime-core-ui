<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useResizeObserver, useDebounceFn } from "@vueuse/core";

import type { ITerminalOptions, ITheme, ITerminalInitOnlyOptions } from "xterm";
import { Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import * as knimeColors from "webapps-common/ui/colors/knimeColors.mjs";

import Button from "webapps-common/ui/components/Button.vue";

export interface ConsoleText {
  text: string;
}

export type ConsoleHandler = (text: ConsoleText) => void;

const DEBOUNCE_TIME = 300;

const theme: ITheme = {
  background: knimeColors.White,
  foreground: knimeColors.Masala,
  selectionBackground: knimeColors.Porcelain,
  cursor: knimeColors.StoneGray,
  black: knimeColors.Black,
  red: knimeColors.HibiscusDark,
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

onMounted(() => {
  term.open(termRef.value as HTMLElement);
  emit("console-created", (text: ConsoleText) => term.write(text.text));
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
