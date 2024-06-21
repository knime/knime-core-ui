<script setup lang="ts">
import { onMounted, ref } from "vue";
import {
  AlertingService,
  ExtensionTypes,
  JsonDataService,
  type UIExtensionService,
} from "@knime/ui-extension-service";
import {
  type ExtensionConfig,
  ResourceTypes,
  UIExtension,
  type UIExtensionAPILayer,
} from "webapps-common/ui/uiExtensions";
import { getScriptingService } from "@/scripting-service";

const baseService = ref<UIExtensionService<UIExtensionAPILayer> | null>(null);
const extensionConfig = ref<ExtensionConfig | null>(null);
const resourceLocation = ref<string>("");
const dataAvailable = ref<boolean>(false);
const numberOfOutputRows = ref<number>(0);

const emit = defineEmits(["output-table-updated"]);

const makeExtensionConfig = async (
  nodeId: string,
  projectId: string,
  workflowId: string,
  baseUrl: string,
): Promise<ExtensionConfig> => ({
  nodeId,
  extensionType: ExtensionTypes.DIALOG,
  projectId,
  workflowId,
  hasNodeView: false,
  resourceInfo: {
    id: "someId",
    type: ResourceTypes.SHADOW_APP,
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
      extensionType: ExtensionTypes.DIALOG,
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
    AlertingService.getInstance().then((service) => service.sendAlert(alert));
  },
  setControlsVisibility: noop,
  setReportingContent: noop,
};
const updateExtensionConfig = async (config: ExtensionConfig) => {
  // @ts-ignore
  resourceLocation.value = `${config.resourceInfo.baseUrl}${config.resourceInfo.path.split("/").slice(0, -1).join("/")}/TableView.js`;

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
    async (numberOfRows) => {
      await updateExtensionConfig(extensionConfigLoaded);
      dataAvailable.value = true;
      numberOfOutputRows.value = numberOfRows;
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
        <div class="preview-warning-text">
          Preview computed on first {{ numberOfOutputRows }} row{{
            numberOfOutputRows === 1 ? "" : "s"
          }}
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
    To see the preview, please evaluate the expression using the button above.
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
  padding: 5px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}

.preview-warning-text {
  background-color: white;
  color: black;
  padding: 2px 8px;
  border-radius: 999vw;
  box-shadow: 0 4px 8px rgb(0 0 0 / 10%);
  text-align: center;
  font-size: small;
  font-weight: bold;
  vertical-align: middle;
}
</style>
