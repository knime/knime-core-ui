<script lang="ts">
import type { ConsoleText, ScriptingService, NodeSettings } from '../utils/scripting-service';
import { defineComponent, ref, inject } from 'vue';
import { Terminal } from 'xterm';

export default defineComponent({
    name: 'OutputConsole',
    inject: ['scriptingService'],
    setup() {
        const term = ref(null);
        term.value = new Terminal({ convertEol: true, disableStdin: true, rows: 10, cols: 80 });
        const scriptingService = inject('scriptingService') as ScriptingService<NodeSettings>;
        return { term, scriptingService };
    },
    mounted() {
        this.scriptingService.registerConsoleEventHandler((text: ConsoleText) => {
            this.term.write(text.text);
        });
        this.term.open(this.$refs.xterm as HTMLElement);
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

