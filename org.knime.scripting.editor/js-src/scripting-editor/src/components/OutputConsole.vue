<script lang="ts">
import Vue from 'vue';
import { ConsoleText, ScriptingService } from '../utils/scripting-service';

export default Vue.extend({
    name: 'OutputConsole',
    inject: ['getScriptingService'],
    data() {
        return {
            lines: []
        } as {
      lines: { text: string; type: 'stdout' | 'stderr'; index: number }[];
    };
    },
    mounted() {
    // TODO(review) is this the right way to make the scripting service available?
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore type inference does not work!
        const scriptingService: ScriptingService = this.getScriptingService();
        scriptingService.registerConsoleEventHandler((text: ConsoleText) => {
            this.lines.push({ ...text, index: this.lines.length });
        });
    }
});
</script>

<template>
  <div class="output-console">
    <ul>
      <li
        v-for="l in lines"
        :key="l.index"
      >
        <div
          v-if="l.stderr"
          class="console-error"
        >
          ERROR: {{ l.text }}
        </div>
        <div v-else>
          {{ l.text }}
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="postcss">
.output-console {
  height: 100px;

  .console-error {
    color: #ff0000;
  }
}
</style>
