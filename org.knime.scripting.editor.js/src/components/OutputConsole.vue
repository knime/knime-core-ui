<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useDebounceFn, useResizeObserver } from "@vueuse/core";
import { FitAddon } from "@xterm/addon-fit";
import { Unicode11Addon } from "@xterm/addon-unicode11";
import type {
  ITerminalInitOnlyOptions,
  ITerminalOptions,
  ITheme,
} from "@xterm/xterm";
import { Terminal } from "@xterm/xterm";
import type { XOR } from "ts-xor";

import * as knimeColors from "@knime/styles/colors/knimeColors";

import useShouldFocusBePainted from "./utils/shouldFocusBePainted";

export type ConsoleText = XOR<
  { text: string },
  { warning: string },
  { error: string }
>;

export type ConsoleHandler = {
  writeln: (text: ConsoleText) => void;
  write: (text: ConsoleText) => void;
  clear: () => void;
};

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

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text);
};

// Attach CTRL+C for COPY on windows and linux
// On mac the default (CMD+C) is already fine
term.attachCustomKeyEventHandler((e) => {
  // NB: We also support uppercase C for CTRL+SHIFT+C
  if ((e.key === "c" || e.key === "C") && e.ctrlKey) {
    const selection = term.getSelection();
    if (selection) {
      copyToClipboard(selection);
    }
    return false;
  } else if (e.key === "Tab") {
    // We don't want xterm to handle this itself, so return false.
    return false;
  } else if (e.key === "Escape") {
    // Blur the terminal, and also return false so xterm doesn't try
    // to handle the keypress.
    term.blur();
    return false;
  }
  return true;
});

// Scroll to bottom whenever something is printed to term
term.onWriteParsed(() => term.scrollToBottom());

const emit =
  defineEmits<(event: "console-created", handler: ConsoleHandler) => void>();

const format = (text: ConsoleText) => {
  if ("error" in text) {
    return `❌ ${ANSI_ERROR_START}${text.error}${ANSI_RESET}`;
  } else if ("warning" in text) {
    return `⚠️  ${ANSI_WARNING_START}${text.warning}${ANSI_RESET}`;
  } else {
    return text.text;
  }
};

onMounted(() => {
  term.open(termRef.value as HTMLElement);
  emit("console-created", {
    write: (text) => term.write(format(text)),
    writeln: (text) => term.writeln(format(text)),
    clear: () => term.reset(),
  });
  const listener = term.onLineFeed(() => {
    fitAddon.fit();
    listener.dispose();
  });
});

onUnmounted(() => {
  term?.dispose();
});

const paintFocus = useShouldFocusBePainted();
</script>

<template>
  <div
    ref="termRef"
    class="terminal"
    :class="{ 'focus-painted': paintFocus }"
  />
</template>

<style lang="postcss">
@import url("@xterm/xterm/css/xterm.css");
</style>

<style lang="postcss" scoped>
.console {
  height: 100%;
}

.terminal.focus-painted:focus-within :deep(> div:first-of-type::after) {
  content: "";
  position: absolute;
  inset: -3px;
  pointer-events: none;
  z-index: 1;
  border: 2px solid var(--knime-cornflower);
}

.terminal {
  height: 100%;
}
</style>
