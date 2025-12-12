<script setup lang="ts">
import { onMounted, ref } from "vue";

import NodeDialogCore from "@knime/core-ui/src/nodeDialog/NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "@knime/core-ui/src/nodeDialog/api/types/RpcTypes";
import useFlowVariableSystem from "@knime/core-ui/src/nodeDialog/composables/useFlowVariableSystem";

import {
  type GenericNodeSettings,
  ScriptingEditor,
  getScriptingService,
  getSettingsService,
} from "../lib/main";
import { getAdditionalSettings } from "../src/init";

import { getAppInitialData } from "./app-initial-data";

const initialData = getAppInitialData();

const additionalSettings = getAdditionalSettings();
console.log("Additional Settings:", additionalSettings);

// ////// NEW

const scriptingService = getScriptingService();
const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const callRpcMethod: NodeDialogCoreRpcMethods = (param): Promise<any> => {
  console.log("RPC Method called:", param);
  return scriptingService.callRpcMethod(param);
};

const toSettings = (commonSettings: GenericNodeSettings) => {
  return {
    ...commonSettings,
    additionalSettings: coreComponent.value?.getDataAndFlowVariableSettings(),
  };
};

const settingsService = getSettingsService();
const registerSettings = settingsService.registerSettings.bind(settingsService);

const { setInitialFlowVariablesMap, flowVariablesMap } = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: () => coreComponent.value?.getCurrentData() ?? {}, // TODO add other data (combined data)
});

onMounted(() => {
  // TODO get the initial flow variables
  setInitialFlowVariablesMap(additionalSettings.flowVariableSettings);
});

const handleAlert = (alert: { type: string; message: string }) => {
  console.log(`Alert received: [${alert.type}] ${alert.message}`);
};
</script>

<template>
  <main>
    <ScriptingEditor
      :language="initialData.language"
      :file-name="initialData.fileName"
      :to-settings="toSettings"
      :initial-pane-sizes="{
        bottom: 0,
        left: 0,
        right: 100,
      }"
    >
      <template #right-pane>
        <NodeDialogCore
          ref="coreComponent"
          :initial-data="additionalSettings"
          :has-node-view="false"
          :call-rpc-method="callRpcMethod"
          :register-settings="registerSettings"
          @alert="handleAlert"
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");
</style>
