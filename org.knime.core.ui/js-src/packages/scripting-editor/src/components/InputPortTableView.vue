<script setup lang="ts">
import { onMounted, onUnmounted, ref, watchEffect } from "vue";

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

interface Props {
  inputNodeId: string;
  portIdx: number;
  viewIdx: number;
}

const props = withDefaults(defineProps<Props>(), {});

const dataAvailable = ref<boolean>(false);

const extensionConfig = ref<ExtensionConfig | null>(null);
const resourceLocation = ref<string | null>(null);
const baseService = ref<UIExtensionService<UIExtensionAPILayer> | null>(null);

const noop = () => {
  /* mock unused api fields */
};
const apiLayer: UIExtensionAPILayer = {
  registerPushEventService: () => () => {},
  callNodeDataService: async ({ dataServiceRequest, serviceType }) => {
    const response = await baseService.value?.callKnimeUiApi!(
      "PortService.callPortDataService",
      {
        nodeId: props.inputNodeId,
        portIdx: props.portIdx,
        viewIdx: props.viewIdx,
        serviceType,
        dataServiceRequest,
      },
    );

    return response?.isSome ? { result: response.result } : { result: null };
  },
  updateDataPointSelection: async ({ mode, selection }) => {
    const response = await baseService.value?.callKnimeUiApi!(
      "PortService.updateDataPointSelection",
      {
        nodeId: props.inputNodeId,
        portIdx: props.portIdx,
        viewIdx: props.viewIdx,
        mode,
        selection,
      },
    );

    return response?.isSome ? { result: response.result } : { result: null };
  },
  getResourceLocation(path) {
    const { baseUrl } = extensionConfig.value!.resourceInfo as unknown as {
      baseUrl: string;
    };
    if (baseUrl) {
      // baseUrl as provided by the backend (port view specific, only available in the desktop environment)
      return Promise.resolve(baseUrl + path);
    } else {
      // ask the ui-extension embedder to resolve the resource location
      return baseService.value!.getResourceLocation(path);
    }
  },
  imageGenerated: noop,
  onApplied: noop,
  onDirtyStateChange: noop,
  publishData: noop,
  sendAlert(alert) {
    AlertingService.getInstance()
      .then((service) =>
        // @ts-expect-error baseService is not API but a property of the service
        service.baseService.sendAlert(alert),
      )
      .catch(() => {});
  },
  setControlsVisibility: noop,
  setReportingContent: noop,
  showDataValueView: noop,
  closeDataValueView: noop,
};

onMounted(async () => {
  baseService.value =
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ((await JsonDataService.getInstance()) as any).baseService;
});

watchEffect(() => {
  if (baseService.value === null) {
    return;
  }

  baseService.value.callKnimeUiApi!("PortService.getPortView", {
    nodeId: props.inputNodeId,
    portIdx: props.portIdx,
    viewIdx: props.viewIdx,
  })
    .then(async (response) => {
      if (response.isSome) {
        extensionConfig.value = response.result as ExtensionConfig;
        const path = extensionConfig.value!.resourceInfo.path!;
        resourceLocation.value = await apiLayer.getResourceLocation(path);
      }
      dataAvailable.value = true;
    })
    .catch((error) => {
      consola.error("Error while fetching data", error);
      dataAvailable.value = false;
    });
});

onUnmounted(() => {
  baseService.value?.callKnimeUiApi!("PortService.deactivatePortDataServices", {
    nodeId: props.inputNodeId,
    portIdx: props.portIdx,
    viewIdx: props.viewIdx,
  }).catch(() => {
    consola.error("InputPortTableView::Failed deactivating port data service", {
      nodeId: props.inputNodeId,
      portIdx: props.portIdx,
      viewIdx: props.viewIdx,
    });
  });
  dataAvailable.value = false;
});
</script>

<template>
  <div class="port-table">
    <UIExtension
      v-if="dataAvailable"
      :api-layer="apiLayer"
      :extension-config="extensionConfig!"
      :resource-location="resourceLocation!"
      :shadow-app-style="{ height: '100%', width: '100%', overflowX: 'scroll' }"
    />
    <div v-else>No data available</div>
  </div>
</template>

<style lang="postcss" scoped>
.port-table {
  height: 100%;
}
</style>
