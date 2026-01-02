<script setup lang="ts">
import { computed, inject, onMounted, ref } from "vue";

import { InputField, Label, NumberInput, SplitPanel } from "@knime/components";
import {
  DialogService,
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import NodeDialogCore from "../nodeDialog/NodeDialogCore.vue";
import type { NodeDialogCoreRpcMethods } from "../nodeDialog/api/types/RpcTypes";
import type { NodeDialogCoreInitialData } from "../nodeDialog/types/InitialData";
import useFlowVariableSystem from "../nodeDialog/composables/useFlowVariableSystem";
import type { flow } from "lodash-es";
import type { JsonSchema } from "@jsonforms/core";
import { defaultRenderers, JsonFormsDialog } from "@knime/jsonforms";

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

// Display mode tracking
const displayMode = ref<"small" | "large">("small");
const isLargeMode = computed(() => displayMode.value === "large");

// Right pane state management
const rightPaneExpanded = ref(true);
const rightPaneSize = ref<number>(300);

// NodeDialogCore
const coreComponent = ref<InstanceType<typeof NodeDialogCore> | null>(null);
const dialogInitialData = ref<NodeDialogCoreInitialData | null>(null);
let dialogService: DialogService;

// Minimal RPC method implementation for demo purposes
const callRpcMethod: NodeDialogCoreRpcMethods = async (method, options) => {
  console.log("RPC method called:", method, options);
  // Return minimal responses for common methods
  return {} as any;
};

/**
 * Index of the column whose context menu is currently active. -1 if none.
 */
const activeColumnIndex = ref<number>(0);

// Flow variable system
const { setInitialFlowVariablesMap} = useFlowVariableSystem({
  callRpcMethod,
  getCurrentData: () => coreComponent.value?.getCurrentData() ?? {},
});

onMounted(async () => {
  const service = getKnimeService();
  dialogService = new DialogService(service);
  displayMode.value = dialogService.getInitialDisplayMode();
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });

  // Get and log initial data
  const jsonDataService = new JsonDataService(service);
  const initialData = await jsonDataService.initialData();
  console.log("Initial data:", initialData);

  // Set dialog initial data if available
  if (initialData) {
    dialogInitialData.value = initialData as NodeDialogCoreInitialData;

    // Initialize flow variables map
    if (dialogInitialData.value.flowVariableSettings) {
      setInitialFlowVariablesMap(dialogInitialData.value.flowVariableSettings);
    }
  }

  dialogService.setApplyListener(() => 
    jsonDataService.applyData({
      data: dialogInitialData.value?.data,
      flowVariableSettings: dialogInitialData.value?.flowVariableSettings,
    })
  )

});

const onChange = ({data}) => {
}
const setStateProviderListener = (...listenerArgs: any) => {
  console.log("State provider listener set:", listenerArgs);
};
</script>

<template>
  <div class="table-creator-dialog">
    <SplitPanel
      v-model:expanded="rightPaneExpanded"
      v-model:secondary-size="rightPaneSize"
      :hide-secondary-pane="!isLargeMode"
      direction="right"
      :secondary-snap-size="220"
      use-pixel
      keep-element-on-close
      class="split-panel"
    >
      <!-- Main content area -->
      <div class="main-content">
        <h1>Table Creator Dialog</h1>
        <p>Current mode: {{ displayMode }}</p>

        <div class="always-visible">
          <p>This is the main content area</p>
          <p>This content is always visible in both small and large modes</p>
        </div>

        <div v-if="!isLargeMode" class="small-mode-hint">
          <p>
            💡 Enlarge the dialog to see additional options in the right panel
          </p>
        </div>
      </div>

      <!-- Right panel - only visible in large mode -->
      <template #secondary>
        <NumberInput 
          v-model="activeColumnIndex"
          type="integer"
        />
        <div v-if="dialogInitialData && activeColumnIndex !== -1" class="right-pane">
          <JsonFormsDialog
            :data="dialogInitialData.data.model!.columns[activeColumnIndex]"
            :schema="dialogInitialData.schema.properties.model.properties.columns.items"
            :uischema="{elements: dialogInitialData.ui_schema.elements[0].options.detail}"
            @change="({data}) => {
              dialogInitialData!.data.model!.columns[activeColumnIndex] = data;
            }"
            :renderers="defaultRenderers"
            @state-provider-listener="(_id, callback) => callback(dialogInitialData?.initialUpdates[0].values[0].value)"
          />
          <NodeDialogCore
            v-if="dialogInitialData"
            ref="coreComponent"
            :initial-data="dialogInitialData"
            :has-node-view="false"
            :call-rpc-method="callRpcMethod"
            :register-settings="dialogService.registerSettings.bind(dialogService)"
          />
          <div v-else class="loading">
            <p>Loading dialog configuration...</p>
          </div>
        </div>
      </template>
    </SplitPanel>
  </div>
</template>

<style scoped>
.table-creator-dialog {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.split-panel {
  height: 100%;
  width: 100%;
  flex-grow: 1;
}

.main-content {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.always-visible {
  padding: 15px;
  background-color: var(--knime-gray-ultra-light, #f5f5f5);
  margin-bottom: 20px;
  border-radius: 4px;
}

.small-mode-hint {
  padding: 15px;
  background-color: var(--knime-yellow-light, #fff9c4);
  border-left: 4px solid var(--knime-yellow, #fdd835);
  border-radius: 4px;
  margin-top: 20px;
}

.small-mode-hint p {
  margin: 0;
}

.right-pane {
  background-color: var(--knime-gray-ultra-light, #fafafa);
  height: 100%;
  overflow-y: auto;
  border-left: 1px solid var(--knime-gray-light, #e0e0e0);
}

.loading {
  padding: 20px;
  text-align: center;
  color: var(--knime-masala, #666);
}
</style>
