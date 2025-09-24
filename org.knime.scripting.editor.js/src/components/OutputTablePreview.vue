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

import { getScriptingService } from "@/init";

const baseService = ref<UIExtensionService<UIExtensionAPILayer> | null>(null);
const extensionConfig = ref<ExtensionConfig | null>(null);
const resourceLocation = ref<string>("");
const dataAvailable = ref<boolean>(false);
// Note that the default value should not be used but it will be overwritten
// when the first preview is available
const previewDisclaimerMessage = ref<string>("Preview");

const emit = defineEmits(["output-table-updated"]);

const makeExtensionConfig = async (
  nodeId: string,
  projectId: string,
  workflowId: string,
  baseUrl: string,
): Promise<ExtensionConfig> => {
  const initialData =
    (await getScriptingService().getOutputPreviewTableInitialData()) ?? "{}";

  return {
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
    initialData: JSON.parse(initialData),
  };
};

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
      // @ts-expect-error baseService is not API but a property of the service
      service.baseService.sendAlert(alert),
    );
  },
  setControlsVisibility: noop,
  setReportingContent: noop,
  showDataValueView: noop,
  closeDataValueView: noop,
};
const updateExtensionConfig = async (config: ExtensionConfig) => {
  // @ts-expect-error baseUrl is not part of the type definition but it exists
  resourceLocation.value = `${config.resourceInfo.baseUrl}${config.resourceInfo.path.split("/").slice(0, -1).join("/")}/core-ui/TableView.js`;

  extensionConfig.value = await makeExtensionConfig(
    config.nodeId,
    config.projectId,
    config.workflowId,
    // @ts-expect-error baseUrl is not part of the type definition but it exists
    config.resourceInfo.baseUrl,
  );
};

onMounted(async () => {
  const jsonService = await JsonDataService.getInstance();
  // @ts-expect-error baseService is not API but a property of the service
  baseService.value = jsonService.baseService;

  const extensionConfigLoaded: ExtensionConfig =
    // @ts-expect-error baseService is not API but a property of the service
    await baseService.value.getConfig();
  await updateExtensionConfig(extensionConfigLoaded);

  getScriptingService().registerEventHandler(
    "updateOutputTable",
    async (updatedPreviewDisclaimerMessage: string) => {
      await updateExtensionConfig(extensionConfigLoaded);
      dataAvailable.value = true;
      previewDisclaimerMessage.value = updatedPreviewDisclaimerMessage;

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
        <div class="preview-warning-text">{{ previewDisclaimerMessage }}</div>
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
