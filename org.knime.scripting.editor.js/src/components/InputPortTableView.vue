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
    return Promise.resolve(
      (extensionConfig.value!.resourceInfo as unknown as { baseUrl: string })
        .baseUrl + path,
    );
  },
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

onMounted(async () => {
  baseService.value = (
    (await JsonDataService.getInstance()) as any
  ).baseService;
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
    .then((response) => {
      if (response.isSome) {
        extensionConfig.value = response.result;
        resourceLocation.value =
          // @ts-expect-error the baseUrl is not part of the type definition but it exists
          extensionConfig.value?.resourceInfo?.baseUrl +
          extensionConfig.value?.resourceInfo?.path;
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
