<script setup lang="ts">
import { onMounted, ref } from "vue";

import {
  type ExtensionConfig,
  UIExtension,
  type UIExtensionAPILayer,
} from "@knime/ui-extension-renderer/vue";
import {
  AlertingService,
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import { getScriptingService } from "@/scripting-service";

const baseService = ref<UIExtensionService<UIExtensionAPILayer> | null>(null);
const extensionConfig = ref<ExtensionConfig | null>(null);
const resourceLocation = ref<string>("");
const dataAvailable = ref<boolean>(false);
const numberOfOutputRows = ref<{
  numberOfRows: number;
  totalNumberOfRows: number;
}>({ numberOfRows: 0, totalNumberOfRows: 0 });

const emit = defineEmits(["output-table-updated"]);

const makeExtensionConfig = async (
  nodeId: string,
  projectId: string,
  workflowId: string,
  baseUrl: string,
): Promise<ExtensionConfig> => ({
  nodeId,
  extensionType: "dialog",
  projectId,
  workflowId,
  hasNodeView: false,
  resourceInfo: {
    id: "someId",
    type: "SHADOW_APP",
    path: `${baseUrl}uiext/tableview/TableView.js`,
  },
  initialData: JSON.parse(
    await (
      await JsonDataService.getInstance()
    ).data({
      method: "OutputPreviewTableInitialDataRpcSupplier.getInitialData",
    }),
  ),
});

const noop = () => {
  /* mock unused api fields */
};
const apiLayer: UIExtensionAPILayer = {
  registerPushEventService: () => () => {},
  callNodeDataService: async ({ dataServiceRequest, serviceType }) => {
    const requestObj = JSON.parse(dataServiceRequest);
    requestObj.method = `TableViewDataService.${requestObj.method}`;

    const response = await baseService.value?.callNodeDataService({
      nodeId: extensionConfig.value!.nodeId,
      workflowId: extensionConfig.value!.workflowId,
      projectId: extensionConfig.value!.projectId,
      extensionType: "dialog",
      serviceType,
      dataServiceRequest: JSON.stringify(requestObj),
    });

    // @ts-ignore
    return response;
  },
  updateDataPointSelection: () => Promise.resolve({ result: null }),
  getResourceLocation: () => Promise.resolve(resourceLocation.value),
  imageGenerated: noop,
  onApplied: noop,
  onDirtyStateChange: noop,
  publishData: noop,
  sendAlert(alert) {
    AlertingService.getInstance().then((service) =>
      // @ts-expect-error
      service.baseService.sendAlert(alert),
    );
  },
  setControlsVisibility: noop,
  setReportingContent: noop,
  showDataValueView: noop,
  closeDataValueView: noop,
};
const updateExtensionConfig = async (config: ExtensionConfig) => {
  // @ts-ignore
  resourceLocation.value = `${config.resourceInfo.baseUrl}${config.resourceInfo.path.split("/").slice(0, -1).join("/")}/core-ui/TableView.js`;

  extensionConfig.value = await makeExtensionConfig(
    config.nodeId,
    config.projectId,
    config.workflowId,
    // @ts-ignore
    config.resourceInfo.baseUrl,
  );
};

onMounted(async () => {
  const jsonService = await JsonDataService.getInstance();
  baseService.value = (jsonService as any).baseService;

  // @ts-ignore
  const extensionConfigLoaded = await baseService.value.getConfig();
  await updateExtensionConfig(extensionConfigLoaded);

  getScriptingService().registerEventHandler(
    "updateOutputTable",
    async (tableUpdateMetadata) => {
      await updateExtensionConfig(extensionConfigLoaded);
      dataAvailable.value = true;
      numberOfOutputRows.value = { ...tableUpdateMetadata };

      emit("output-table-updated");
    },
  );
});
</script>

<template>
  <div
    v-if="dataAvailable === true && extensionConfig !== null"
    class="output-table"
  >
    <div class="output-table-preview">
      <div class="preview-background">
        <div
          v-if="
            numberOfOutputRows.numberOfRows !=
            numberOfOutputRows.totalNumberOfRows
          "
          class="preview-warning-text"
        >
          Preview computed on first {{ numberOfOutputRows.numberOfRows }}
          {{ numberOfOutputRows.numberOfRows === 1 ? "row" : "rows" }} of
          {{ numberOfOutputRows.totalNumberOfRows }}
          {{ numberOfOutputRows.totalNumberOfRows === 1 ? "row" : "rows" }}
        </div>
        <div v-else class="preview-warning-text">
          Preview computed on all rows.
        </div>
      </div>
      <UIExtension
        :api-layer="apiLayer"
        :extension-config="extensionConfig"
        :resource-location="resourceLocation"
        :shadow-app-style="{ height: '100%', width: '100%' }"
      />
    </div>
  </div>
  <div v-else class="output-table-preview pre-evaluation-sign">
    To see the preview, evaluate the expression using the button above.
  </div>
</template>

<style lang="postcss" scoped>
.output-table-preview {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.pre-evaluation-sign {
  display: flex;
  height: 35px;
  justify-content: center;
  border-radius: 10px;
}

.output-table {
  height: 100%;
  width: 100%;
}

.preview-background {
  width: 100%;
  background-color: var(--knime-cornflower-semi);
  padding: var(--space-4) 0;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}

.preview-warning-text {
  background-color: white;
  color: black;
  padding: var(--space-4) var(--space-8);
  border-radius: 999vw;
  box-shadow: 0 4px 8px rgb(0 0 0 / 10%);
  text-align: center;
  font-size: small;
  vertical-align: middle;
}
</style>
