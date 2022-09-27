<script lang="ts">
import { defineComponent } from 'vue';
import type { ScriptingService } from '../utils/scripting-service';
import type { ConsoleText } from '../utils/scripting-service';
import { Terminal } from 'xterm';

export default defineComponent({
    name: 'OutputConsole',
    inject: ['getScriptingService'],
    mounted() {
        // TODO(review) is this the right way to make the scripting service available?
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore type inference does not work!
        const scriptingService: ScriptingService = this.getScriptingService();

        // Init XTerm js
        const term = new Terminal({ convertEol: true, disableStdin: true, rows: 10, cols: 80 });
        term.open(this.$refs.xterm as HTMLElement);

        // TODO(AP-19343) resize the terminal with the window (maybe use xterm-addon-fit)

        // Send text to the terminal
        scriptingService.registerConsoleEventHandler((text: ConsoleText) => {
            term.write(text.text);
        });
    }
});
</script>

<template>
  <div
    ref="xterm"
    class="terminal"
  />
</template>

<style>
@import 'xterm/css/xterm.css';
</style>
