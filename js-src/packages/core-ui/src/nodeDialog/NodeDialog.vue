<script setup lang="ts">
import { inject, onMounted, ref } from "vue";
import { cloneDeep } from "lodash-es";

import { type UIExtensionService } from "@knime/ui-extension-service";

import NodeDialogCore from "./NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "./api/types/RpcTypes";
import useProvidedFlowVariablesMap from "./composables/components/useProvidedFlowVariablesMap";
import useServices from "./composables/nodeDialog/useServices";
import type { NodeDialogInitialData } from "./types/InitialData";
import type { SettingsData } from "./types/SettingsData";

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

const { dialogService, jsonDataService, sharedDataService, alertingService } =
  useServices(getKnimeService());

// State for initial data
const initialData = ref<NodeDialogInitialData>();
const hasNodeView = ref(false);

const { flowVariablesMap, setInitialFlowVariablesMap } =
  useProvidedFlowVariablesMap();

const handlePublishData = (newData: SettingsData) => {
  const publishedData = cloneDeep({
    data: newData,
    flowVariableSettings: flowVariablesMap,
  });
  sharedDataService!.shareData(publishedData);
};

const handleAlert: typeof alertingService.sendAlert = (...args) =>
  alertingService.sendAlert(...args);
const registerSettings: typeof dialogService.registerSettings = (...args) =>
  dialogService.registerSettings(...args);

const callRpcMethod = ((...args) =>
  jsonDataService.data(...args)) as NodeDialogCoreRpcMethods;

const handleSetControlsVisibility = (visible: boolean) => {
  dialogService?.setControlsVisibility({
    shouldBeVisible: visible,
  });
};

const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);

const callApplyData = async (dataTransformer) => {
  const data = coreComponent.value?.getDataAndFlowVariableSettings();
  if (data) {
    if (dataTransformer) {
      const transformed = cloneDeep(data.data);
      dataTransformer(transformed);
      data.data = transformed;
    }
    await jsonDataService!.applyData(data);
  }
};
onMounted(async () => {
  const initialSettings =
    (await jsonDataService.initialData()) as NodeDialogInitialData;
  setInitialFlowVariablesMap(initialSettings.flowVariableSettings);
  hasNodeView.value = dialogService.hasNodeView();
  initialData.value = initialSettings;

  // Set up apply listener
  dialogService.setApplyListener(() =>
    jsonDataService!.applyData(
      coreComponent.value?.getDataAndFlowVariableSettings(),
    ),
  );
});

/**
 * Expose methods for testing - delegate to core component
 */
defineExpose({
  // Expose core component ref for full access in tests
  getCurrentData: () => coreComponent.value?.getCurrentData(),
  flowVariablesMap,
  coreComponent,
  jsonDataService,
});
</script>

<template>
  <NodeDialogCore
    v-if="initialData"
    ref="coreComponent"
    :initial-data="initialData"
    :has-node-view="hasNodeView"
    :call-rpc-method="callRpcMethod"
    :call-apply-data="callApplyData"
    :register-settings="registerSettings"
    @publish-data="handlePublishData"
    @alert="handleAlert"
    @set-controls-visibility="handleSetControlsVisibility"
  />
</template>
