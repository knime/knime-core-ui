<script setup lang="ts">
import { ref } from "vue";

import { Button, type MenuItem } from "@knime/components";
import SettingsIcon from "@knime/styles/img/icons/cog.svg";
import FileTextIcon from "@knime/styles/img/icons/file-text.svg";

import {
  type ConsoleHandler,
  type GenericNodeSettings,
  OutputConsole,
  ScriptingEditor,
  type SettingsMenuItem,
  consoleHandler,
  initConsoleEventHandler,
  setConsoleHandler,
} from "../lib/main";

import DebugToolbar from "./DebugToolbar.vue";

const menuItems: MenuItem[] = [
  {
    text: "Set Settings",
    icon: SettingsIcon,
    showSettingsPage: true,
    title: "Demo Settings",
    separator: true,
  } as SettingsMenuItem,
  {
    text: "Documentation",
    icon: FileTextIcon,
    href: "https://docs.knime.com/",
  },
];

const currentSettingsMenuItem = ref<any>(null);

const onMenuItemClicked = ({ item }: { item: any }) => {
  currentSettingsMenuItem.value = item;
};

const toSettings = (commonSettings: GenericNodeSettings) => ({
  ...commonSettings,
  // Add any additional demo-specific settings here
});

// Mock functions for demo purposes
const runScript = () => {
  consola.log("Demo: Running script...");
  consoleHandler.writeln({ text: "Demo: Script executed successfully!" });
};

const runSelectedLines = () => {
  consola.log("Demo: Running selected lines...");
  consoleHandler.writeln({ text: "Demo: Selected lines executed!" });
};
</script>

<template>
  <main>
    <DebugToolbar />
    <ScriptingEditor
      title="Demo Scripting Editor"
      language="python"
      file-name="demo.py"
      :menu-items="menuItems"
      :to-settings="toSettings"
      :additional-bottom-pane-tab-content="[
        {
          slotName: 'bottomPaneTabSlot:console',
          label: 'Console',
        },
      ]"
      @menu-item-clicked="onMenuItemClicked"
    >
      <template #settings-title>
        {{ currentSettingsMenuItem?.title }}
      </template>
      <template #settings-content>
        <div style="padding: 20px">
          <h3>Demo Settings</h3>
          <p>
            This is a demo settings page. In a real application, you would put
            your settings controls here.
          </p>
          <ul>
            <li>Setting 1: Enabled</li>
            <li>Setting 2: Disabled</li>
            <li>Setting 3: Auto</li>
          </ul>
        </div>
      </template>

      <template #code-editor-controls="{ showButtonText }">
        <div style="display: flex; gap: 8px">
          <Button compact with-border @click="runSelectedLines">
            {{ showButtonText ? "Run Selection" : "⏵" }}
          </Button>
          <Button compact primary @click="runScript">
            {{ showButtonText ? "Run Script" : "▶" }}
          </Button>
        </div>
      </template>

      <template #right-pane>
        <div style="padding: 20px; height: 100%; overflow-y: auto">
          <h3>Demo Right Pane</h3>
          <p>This is the right pane where you can display:</p>
          <ul>
            <li>Variable explorer</li>
            <li>Function catalog</li>
            <li>Help documentation</li>
            <li>Preview panels</li>
          </ul>

          <h4>Sample Variables</h4>
          <div
            style="font-family: monospace; padding: 10px; border-radius: 4px"
          >
            <div>input_table: DataFrame (100 rows, 3 columns)</div>
            <div>output_table: DataFrame (100 rows, 4 columns)</div>
            <div>demo_column: Series (100 values)</div>
          </div>

          <h4>Sample Functions</h4>
          <div
            style="font-family: monospace; padding: 10px; border-radius: 4px"
          >
            <div>knio.input_tables[0]</div>
            <div>knio.output_tables[0]</div>
            <div>pd.DataFrame()</div>
            <div>np.array()</div>
          </div>
        </div>
      </template>

      <template #bottom-pane-status-label>
        <span style="color: green">Demo: Ready</span>
      </template>

      <template #bottomPaneTabSlot:console>
        <OutputConsole
          class="console"
          @console-created="
            (console: ConsoleHandler) => {
              setConsoleHandler(console);
              initConsoleEventHandler();
            }
          "
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");

.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.console {
  height: 100%;
}
</style>
