<script setup lang="ts">
import { inject, nextTick, onMounted, provide, ref } from "vue";
import { type JsonSchema, type UISchemaElement } from "@jsonforms/core";
import { cloneDeep, set } from "lodash-es";

import {
  type AlertParams as JsonFormsAlertParams,
  JsonFormsDialog,
  type NamedRenderer,
} from "@knime/jsonforms";
import { type UIExtensionService } from "@knime/ui-extension-service";

import * as flowVariablesApi from "./api/flowVariables";
import type { FlowSettings } from "./api/types";
import useProvidedFlowVariablesMap from "./composables/components/useProvidedFlowVariablesMap";
import {
  type ArrayRecord,
  createArrayAtPath,
} from "./composables/nodeDialog/useArrayIds";
import useCurrentData from "./composables/nodeDialog/useCurrentData";
import { provideAndGetSetupMethod } from "./composables/nodeDialog/useDirtySettings";
import useGlobalWatchers from "./composables/nodeDialog/useGlobalWatchers";
import useServices from "./composables/nodeDialog/useServices";
import useStateProviders from "./composables/nodeDialog/useStateProviders";
import useTriggers, {
  type TriggerCallback,
} from "./composables/nodeDialog/useTriggers";
import useUpdates from "./composables/nodeDialog/useUpdates";
import { initializeRenderers } from "./renderers";
import type { InitialData } from "./types/InitialData";
import type { PersistSchema } from "./types/Persist";
import type { SettingsData } from "./types/SettingsData";
import type { Update, UpdateResult } from "./types/Update";
import type {
  Provided,
  ProvidedByNodeDialog,
  ProvidedForFlowVariables,
} from "./types/provided";
import { hasAdvancedOptions } from "./utils";
import { getConfigPaths } from "./utils/paths";

const { setCurrentData, getCurrentData } = useCurrentData();
const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

const { dialogService, jsonDataService, sharedDataService, alertingService } =
  useServices(getKnimeService());

const { flowVariablesMap, setInitialFlowVariablesMap } =
  useProvidedFlowVariablesMap();

const getData = () => ({
  data: getCurrentData(),
  flowVariableSettings: flowVariablesMap,
});

const publishSettings = () => {
  const publishedData = cloneDeep(getData());
  sharedDataService!.shareData(publishedData);
};
const sendAlert = alertingService.sendAlert.bind(alertingService);
const sendJsonFormsAlert = (alert: JsonFormsAlertParams) =>
  sendAlert({
    message: alert.message,
    type: alert.type === "error" ? "error" : "warn",
    details: alert.details,
  });
const globalArrayIdsRecord: ArrayRecord = {};
const {
  addStateProviderListener,
  callStateProviderListener,
  callStateProviderListenerByIndices,
} = useStateProviders();
const {
  registerWatcher: registerWatcherInternal,
  updateData: updateDataInternal,
  updateDataMultiplePaths: updateDataMultiplePathsInternal,
} = useGlobalWatchers(globalArrayIdsRecord);

const persistSchema = ref<PersistSchema | null>(null);

// WATCHERS
const registerWatcher = async ({
  transformSettings,
  init,
  dependencies,
}: Parameters<Provided["registerWatcher"]>[0]) => {
  const removeWatcher = registerWatcherInternal({
    transformSettings: () => async (dependencyData) => {
      const settingsConsumer = await transformSettings(dependencyData);
      return (newSettings) => {
        settingsConsumer?.(newSettings);
        return newSettings;
      };
    },
    dependencies,
  });
  if (typeof init === "function") {
    await init(getCurrentData());
  }
  return removeWatcher;
};
const updateData = (path: string) => updateDataInternal(path, getCurrentData());
const updateDataMultiplePaths = (paths: string[]) =>
  updateDataMultiplePathsInternal(paths, getCurrentData());

// TRIGGERS
const { registerTrigger, getTriggerCallback, getTriggerIsActiveCallback } =
  useTriggers();

/**
 * We need to use getCurrentData() twice when running triggers:
 * Once for setting the dependencies and once the transformation is to be performed on them.
 * The settings might have changed in the meantime.
 */
const runWithDependencies = async (
  triggerCallback: ReturnType<TriggerCallback>,
) => (await triggerCallback(getCurrentData()))(getCurrentData());

const trigger = (params: unknown) =>
  runWithDependencies(
    getTriggerCallback(params as { id: string; indexIds?: string[] }),
  );
const isTriggerActive = (params: { id: string; indexIds?: string[] }) =>
  getTriggerIsActiveCallback(params)(getCurrentData());
// UPDATES

const pathIsControlledByFlowVariable = (path: string) =>
  getConfigPaths({ path, persistSchema: persistSchema.value ?? {} })
    .flatMap(({ configPath, deprecatedConfigPaths }) => [
      configPath,
      ...deprecatedConfigPaths,
    ])
    .map((configPath) =>
      Boolean(flowVariablesMap[configPath]?.controllingFlowVariableName),
    )
    .includes(true);

const jsonforms = ref<null | InstanceType<typeof JsonFormsDialog>>(null);

const { registerUpdates, resolveUpdateResults } = useUpdates({
  callStateProviderListener,
  callStateProviderListenerByIndices,
  registerTrigger,
  registerWatcher: registerWatcherInternal,
  updateData: updateDataMultiplePaths,
  sendAlert,
  pathIsControlledByFlowVariable,
  setValueAtPath: (path: string, data: any) => {
    if (jsonforms.value) {
      jsonforms.value?.updateData?.(path, data);
    } else {
      set(getCurrentData(), path, data);
      publishSettings();
    }
  },
  globalArrayIdsRecord,
});
const resolveInitialUpdates = (initialUpdates: UpdateResult[]) =>
  resolveUpdateResults(initialUpdates, getCurrentData());
const registerGlobalUpdates = (globalUpdates: Update[]) => {
  const initialTransformation = registerUpdates(globalUpdates);
  if (initialTransformation) {
    // we need to wait for the next tick to ensure that array items are already rendered and have an _id
    nextTick(() => {
      runWithDependencies(initialTransformation);
    });
  }
};

// dirty settings
const { setRegisterSettingsMethod } = provideAndGetSetupMethod();
const setSubPanelExpanded = ({ isExpanded }: { isExpanded: boolean }) => {
  dialogService?.setControlsVisibility({
    shouldBeVisible: !isExpanded,
  });
};
const showAdvancedSettings = ref(false);

const flawedControllingVariablePaths = ref(new Set<string>());
const possiblyFlawedControllingVariablePaths = ref(new Set<string>());
const schema = ref<JsonSchema | null>(null);
const uischema = ref<UISchemaElement | null>(null);
const ready = ref(false);

const getPersistSchema = () => persistSchema.value ?? {};

const callDataService = (parameters: Parameters<Provided["getData"]>[0]) =>
  jsonDataService?.data(parameters as { method?: string; options?: unknown })!;

const getAvailableFlowVariables = (persistPath: string) =>
  flowVariablesApi.getAvailableFlowVariables(
    callDataService,
    persistPath,
    getData(),
  );

const initializeFlowVariablesMap = ({
  flowVariableSettings,
}: {
  flowVariableSettings: Record<string, FlowSettings>;
}) => {
  Object.keys(flowVariableSettings).forEach((persistPath) => {
    if (flowVariableSettings[persistPath].controllingFlowVariableName) {
      possiblyFlawedControllingVariablePaths.value.add(persistPath);
      /**
       * The variable could be valid again since the last time it has been applied.
       */
      delete flowVariableSettings[persistPath].controllingFlowVariableFlawed;
    }
  });
  return flowVariableSettings;
};

const getFlowVariableOverrideValue = async (
  persistPath: string,
  dataPath: string,
) => {
  const { data, flowVariableSettings } = cloneDeep(getData());
  [
    ...flawedControllingVariablePaths.value,
    ...possiblyFlawedControllingVariablePaths.value,
  ].forEach((path) => {
    if (path !== persistPath) {
      delete flowVariableSettings[path];
    }
  });
  const overrideValue = await flowVariablesApi.getFlowVariableOverrideValue(
    callDataService,
    dataPath,
    { data, flowVariableSettings },
  );
  const valid = typeof overrideValue !== "undefined";
  const flowSettings = flowVariablesMap[persistPath];
  if (flowSettings) {
    if (valid) {
      delete flowSettings.controllingFlowVariableFlawed;
    } else {
      flowSettings.controllingFlowVariableFlawed = true;
    }
  }
  flawedControllingVariablePaths.value[valid ? "delete" : "add"](persistPath);
  possiblyFlawedControllingVariablePaths.value.delete(persistPath);
  return overrideValue;
};

const clearControllingFlowVariable = (persistPath: string) => {
  flawedControllingVariablePaths.value.delete(persistPath);
  possiblyFlawedControllingVariablePaths.value.delete(persistPath);
};

const performExternalValidation = async (id: string, value: any) => {
  const receivedData = await callDataService({
    method: "settings.performExternalValidation",
    options: [id, value],
  });
  return receivedData.result || null;
};

const onSettingsChanged = ({ data }: { data: unknown }) => {
  if (data) {
    setCurrentData(data as SettingsData);
    publishSettings();
  }
};

const applySettings = () => jsonDataService!.applyData(getData());

const changeAdvancedSettings = () => {
  if (schema.value === null) {
    return;
  }
  showAdvancedSettings.value = !showAdvancedSettings.value;
};

const hasAdvancedOptions2 = () => {
  if (!uischema.value) {
    return false;
  }
  return hasAdvancedOptions(uischema.value);
};

const isSomeModifierKeyPressed = (event: KeyboardEvent) =>
  (["altKey", "ctrlKey", "metaKey", "shiftKey"] as const).some(
    (key) => event[key],
  );

const onKeydownAdvancedSettingsLink = (event: KeyboardEvent) => {
  if (
    !isSomeModifierKeyPressed(event) &&
    (event.code === "Enter" || event.code === "Space")
  ) {
    event.preventDefault();
    changeAdvancedSettings();
  }
};

const renderers = ref<null | readonly NamedRenderer[]>(null);

onMounted(async () => {
  const initialSettings = (await jsonDataService.initialData()) as InitialData;
  setInitialFlowVariablesMap(initializeFlowVariablesMap(initialSettings));
  schema.value = initialSettings.schema;
  uischema.value = initialSettings.ui_schema;
  setCurrentData(initialSettings.data);
  setRegisterSettingsMethod(dialogService.registerSettings.bind(dialogService));
  persistSchema.value = initialSettings.persist;
  resolveInitialUpdates(initialSettings.initialUpdates ?? []);
  registerGlobalUpdates(initialSettings.globalUpdates ?? []);
  dialogService.setApplyListener(applySettings);
  const hasNodeView = dialogService.hasNodeView();
  renderers.value = initializeRenderers({
    hasNodeView,
    showAdvancedSettings,
    performExternalValidation,
  });
  ready.value = true;
});

const dialogPopoverTeleportDest = ref<null | HTMLElement>(null);

const provided: ProvidedByNodeDialog & ProvidedForFlowVariables = {
  isTriggerActive,
  registerWatcher,
  getData: jsonDataService.data.bind(jsonDataService),
  flowVariablesApi: {
    getAvailableFlowVariables,
    getFlowVariableOverrideValue,
    clearControllingFlowVariable,
  },
  getPersistSchema,
  createArrayAtPath: (path: string) =>
    createArrayAtPath(globalArrayIdsRecord, path),
  getDialogPopoverTeleportDest: () => dialogPopoverTeleportDest.value,
  setSubPanelExpanded,
  updateData,
};

Object.entries(provided).forEach(([key, value]) => {
  provide(key, value);
});

/**
 * Expose the provided objects for testing:
 */
defineExpose({
  ...provided,
  // other test methods:
  setCurrentData,
  getCurrentData,
  jsonDataService,
  callDataService,
  sendAlert,
  schema,
  getAvailableFlowVariables,
  flowVariablesMap,
  getFlowVariableOverrideValue,
  trigger,
  addStateProviderListener,
  flawedControllingVariablePaths,
  possiblyFlawedControllingVariablePaths,
});
</script>

<template>
  <JsonFormsDialog
    v-if="ready"
    ref="jsonforms"
    :data="getCurrentData()"
    :ready="ready"
    :schema="schema!"
    :uischema="uischema!"
    :renderers="renderers!"
    @change="onSettingsChanged"
    @alert="sendJsonFormsAlert"
    @trigger="trigger"
    @update-data="updateData"
    @state-provider-listener="addStateProviderListener"
  >
    <template #top>
      <div ref="dialogPopoverTeleportDest" class="popover-container" />
    </template>
    <template #bottom>
      <a
        v-if="hasAdvancedOptions2()"
        class="advanced-options"
        tabindex="0"
        role="button"
        @click="changeAdvancedSettings"
        @keydown="onKeydownAdvancedSettingsLink"
      >
        {{ showAdvancedSettings ? "Hide" : "Show" }} advanced settings
      </a>
    </template>
  </JsonFormsDialog>
</template>

<style lang="postcss" scoped>
.advanced-options {
  display: flex;
  justify-content: space-between;
  text-decoration: underline;
  margin-top: var(--space-32);
  font-size: 13px;
  cursor: pointer;
  color: var(--knime-dove-gray);

  &:hover {
    color: var(--knime-masala);
    text-decoration: underline dotted;
  }
}

.popover-container {
  position: relative;
  width: 100%;
}
</style>

<style>
@import url("@knime/jsonforms/styles");
</style>
