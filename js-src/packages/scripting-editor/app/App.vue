<script setup lang="ts">
import { onMounted, ref } from "vue";

import NodeDialogCore from "@knime/core-ui/src/nodeDialog/NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "@knime/core-ui/src/nodeDialog/api/types/RpcTypes";
import useFlowVariableSystem from "@knime/core-ui/src/nodeDialog/composables/useFlowVariableSystem";

import {
  ScriptingEditor,
  getScriptingService,
  getSettingsService,
} from "../lib/main";

import { getAppInitialData } from "./app-initial-data";

const scriptingService = getScriptingService();
const settingsService = getSettingsService();

const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const callRpcMethod = scriptingService.callRpcMethod.bind(
  scriptingService,
) as NodeDialogCoreRpcMethods;

const initialData = getAppInitialData();

// Settings

if (settingsService.getSettingsInitialData() === undefined) {
  scriptingService.sendAlert({
    type: "error",
    message: "Settings initial data is undefined",
  });
}
const settingsInitialData = settingsService.getSettingsInitialData()!;
const toSettings = (commonSettings: { script: string }) => {
  if (!coreComponent.value) {
    return { data: { model: { script: commonSettings.script } } };
  }

  const coreDialogSettings =
    coreComponent.value.getDataAndFlowVariableSettings();
  if (
    typeof coreDialogSettings.data.model !== "undefined" &&
    "script" in coreDialogSettings.data.model
  ) {
    coreDialogSettings.data.model.script = commonSettings.script;
  } else {
    throw new Error(
      "Could not find 'model.script' in core dialog settings to update script.",
    );
  }
  return coreDialogSettings;
};

// Flow variable system

const { setInitialFlowVariablesMap } = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: () => coreComponent.value?.getCurrentData() ?? {},
});

onMounted(() => {
  setInitialFlowVariablesMap(settingsInitialData.flowVariableSettings);
});
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
      :show-control-bar="false"
    >
      <template #right-pane>
        <NodeDialogCore
          ref="coreComponent"
          :initial-data="settingsInitialData"
          :has-node-view="false"
          :call-rpc-method="callRpcMethod"
          :register-settings="
            settingsService.registerSettings.bind(settingsService)
          "
          @alert="scriptingService.sendAlert.bind(scriptingService)"
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");
</style>
