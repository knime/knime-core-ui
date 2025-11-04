<script setup lang="ts">
import { onMounted, ref } from "vue";

import { Button, type MenuItem } from "@knime/components";
import SettingsIcon from "@knime/styles/img/icons/cog.svg";
import FileTextIcon from "@knime/styles/img/icons/file-text.svg";
import NodeDialogCore from "@knime/core-ui/src/nodeDialog/NodeDialogCore.vue";
import useFlowVariableSystem from "@knime/core-ui/src/nodeDialog/composables/useFlowVariableSystem";

import {
  type ConsoleHandler,
  type GenericNodeSettings,
  OutputConsole,
  ScriptingEditor,
  type SettingsMenuItem,
  consoleHandler,
  getSettingsService,
  init,
  initConsoleEventHandler,
  setConsoleHandler,
} from "../lib/main";

import DebugToolbar from "./DebugToolbar.vue";
import type { NodeDialogCoreRpcMethods } from "@knime/core-ui/src/nodeDialog/api/types/RpcTypes";
import type { InitialData } from "@knime/core-ui/src/nodeDialog/types/InitialData";

import initialDialogData from "./initialSettingsData.json";

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

//////// NEW

const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const callRpcMethod: NodeDialogCoreRpcMethods = ({
  method,
  options,
}): Promise<any> => {
  console.log(`RPC Method called: ${method} with options:`, options);
  return Promise.resolve({});
};

const settingsService = getSettingsService();
const registerSettings = settingsService.registerSettings.bind(settingsService);

const { setInitialFlowVariablesMap, flowVariablesMap } = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: () => coreComponent.value?.getCurrentData() ?? {}, // TODO add other data (combined data)
});

onMounted(() => {
  setInitialFlowVariablesMap({
    disabled: {
      controllingFlowVariableName: "myVar",
      controllingFlowVariableAvailable: true,
      exposedFlowVariableName: null,
    },
  });
});

const handleAlert = (alert: { type: string; message: string }) => {
  console.log(`Alert received: [${alert.type}] ${alert.message}`);
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
      :initial-pane-sizes="{
        bottom: 0,
        left: 0,
        right: 100,
      }"
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
        <NodeDialogCore
          v-if="initialDialogData"
          ref="coreComponent"
          :initial-data="initialDialogData"
          :has-node-view="false"
          :call-rpc-method="callRpcMethod"
          :register-settings="registerSettings"
          @alert="handleAlert"
        />
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
