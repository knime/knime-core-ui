<script setup lang="ts">
import { onMounted, ref } from "vue";

import NodeDialogCore from "@knime/core-ui/src/nodeDialog/NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "@knime/core-ui/src/nodeDialog/api/types/RpcTypes";
import useFlowVariableSystem from "@knime/core-ui/src/nodeDialog/composables/useFlowVariableSystem";

import { getScriptingService, getSettingsService } from "../init";

const scriptingService = getScriptingService();
const settingsService = getSettingsService();

const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const callRpcMethod = scriptingService.callRpcMethod.bind(
  scriptingService,
) as NodeDialogCoreRpcMethods;

const settingsInitialData = settingsService.getSettingsInitialData()!;
if (settingsInitialData === undefined) {
  scriptingService.sendAlert({
    type: "error",
    message: "Settings initial data is undefined",
  });
}
defineExpose({
  getDataAndFlowVariableSettings: () =>
    coreComponent.value?.getDataAndFlowVariableSettings(),
});

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
  <NodeDialogCore
    ref="coreComponent"
    :initial-data="settingsInitialData"
    :has-node-view="false"
    :call-rpc-method="callRpcMethod"
    :register-settings="settingsService.registerSettings.bind(settingsService)"
    @alert="scriptingService.sendAlert.bind(scriptingService)"
  />
</template>
