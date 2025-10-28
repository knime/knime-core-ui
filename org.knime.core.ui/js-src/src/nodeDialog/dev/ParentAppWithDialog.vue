<script setup lang="ts">
/**
 * Demo: Using NodeDialogCore as a subcomponent in another app
 * (e.g., SQL Editor showing additional settings)
 */
import { computed, onMounted, provide, ref, watch } from "vue";

import type { UIExtensionService } from "@knime/ui-extension-service";

import NodeDialogCore from "../NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "../api/types/RpcTypes";
import { settingStateToFlowVariablesForSettings } from "../composables/nodeDialog/useDirtySettings";
import useServices from "../composables/nodeDialog/useServices";
import useFlowVariableSystem from "../composables/useFlowVariableSystem";
import type { NodeDialogInitialData } from "../types/InitialData";
import FlowVariableButtonWrapper from "../uiComponents/flowVariables/components/FlowVariableButtonWrapper.vue";

const props = defineProps<{
  baseService: UIExtensionService;
  initialDialogData: NodeDialogInitialData;
}>();

const { jsonDataService, alertingService, dialogService } = useServices(
  props.baseService,
);
const callRpcMethod: NodeDialogCoreRpcMethods =
  jsonDataService.data.bind(jsonDataService);
const handleAlert = alertingService.sendAlert.bind(alertingService);
const registerSettings = dialogService.registerSettings.bind(dialogService);

const sqlQuery = ref("SELECT * FROM my_table WHERE id > 100");
const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);

const getCombinedData = () => {
  return {
    sqlQuery: sqlQuery.value,
    ...coreComponent.value?.getCurrentData(),
  };
};

const sqlQuerySettingState = dialogService.registerSettings("model")({
  initialValue: sqlQuery.value,
});
watch(sqlQuery, (newVal) => {
  sqlQuerySettingState.setValue(newVal);
});
const setSqlQuery = (_path: string, newVal: unknown) => {
  sqlQuery.value = newVal as string;
};

const { setInitialFlowVariablesMap, flowVariablesMap } = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: getCombinedData,
});
const flowVariableForSqlQuery = settingStateToFlowVariablesForSettings({
  settingState: sqlQuerySettingState,
  persistPaths: ["sql_query"],
  flowVariablesMap,
});
const sqlQueryIsOverwrittenByFlowVar = computed(() =>
  Boolean(flowVariablesMap.sql_query?.controllingFlowVariableName),
);

const teleportDestination = ref<HTMLElement | null>(null);
provide("getDialogPopoverTeleportDest", () => teleportDestination.value);

onMounted(() => {
  setInitialFlowVariablesMap(props.initialDialogData.flowVariableSettings);
});
</script>

<template>
  <div ref="teleportDestination" />
  <div class="parent-app">
    <h2>Parent App with Embedded NodeDialogCore</h2>
    <p class="description">
      This demonstrates reusing NodeDialogCore inside another app (like SQL
      Editor). The parent app has its own UI and embeds the dialog for
      "additional settings".
    </p>

    <!-- Parent app's own UI (e.g., SQL Editor) -->
    <div class="parent-ui">
      <h3>SQL Editor</h3>
      <textarea
        v-model="sqlQuery"
        rows="3"
        placeholder="Enter SQL query..."
        :disabled="sqlQueryIsOverwrittenByFlowVar"
      />
      <FlowVariableButtonWrapper
        :config-paths="[
          {
            configPath: 'sql_query',
            dataPath: 'sqlQuery',
            deprecatedConfigPaths: [],
          },
        ]"
        data-path="sqlQuery"
        :flow-variables-for-settings="flowVariableForSqlQuery"
        :hover="true"
        @controlling-flow-variable-set="setSqlQuery"
      />
    </div>

    <hr />

    <!-- Embedded NodeDialogCore for "additional settings" -->
    <div class="embedded-dialog">
      <h3>Additional Settings (NodeDialogCore)</h3>
      <NodeDialogCore
        v-if="initialDialogData"
        ref="coreComponent"
        :initial-data="initialDialogData"
        :has-node-view="false"
        :call-rpc-method="callRpcMethod"
        :register-settings="registerSettings"
        @alert="handleAlert"
      />
    </div>

    <!-- Show combined data -->
    <details class="combined-data">
      <summary>Combined Data (Parent + Dialog)</summary>
      <h6>Data:</h6>
      <pre>{{ getCombinedData() }}</pre>
      <h6>Flow Variables:</h6>
      <pre>{{ flowVariablesMap }}</pre>
    </details>
  </div>
</template>

<style scoped>
.parent-app {
  padding: 16px;
}

.description {
  padding: 8px;
  margin-bottom: 16px;
}

.parent-ui {
  margin-bottom: 16px;
}

.parent-ui h3 {
  margin-top: 0;
}

.parent-ui textarea {
  width: 100%;
  padding: 8px;
  font-family: monospace;
  font-size: 14px;
}

hr {
  margin: 24px 0;
  border: none;
  border-top: 2px dashed grey;
}

.embedded-dialog {
  margin-bottom: 16px;
}

.combined-data {
  margin-top: 16px;
  padding: 8px;
  border: 1px solid grey;
}

.combined-data pre {
  overflow: auto;
  font-size: 12px;
}
</style>
