<script setup lang="ts">
import { inject, nextTick, onMounted, provide, ref } from "vue";
import { type JsonSchema, type UISchemaElement } from "@jsonforms/core";
import { cloneDeep, set } from "lodash-es";

import {
  type AlertParams as JsonFormsAlertParams,
  JsonFormsDialog,
  type NamedRenderer,
} from "@knime/jsonforms";
import {
  AlertType,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import * as flowVariablesApi from "./api/flowVariables";
import getChoices from "./api/getChoices";
import type { FlowSettings } from "./api/types";
import useProvidedFlowVariablesMap from "./composables/components/useProvidedFlowVariablesMap";
import {
  createArrayAtPath,
  getArrayIdsRecord,
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
import type { Control } from "./types/Control";
import type { InitialData } from "./types/InitialData";
import type { PersistSchema } from "./types/Persist";
import type { SettingsData } from "./types/SettingsData";
import type { Update, UpdateResult } from "./types/Update";
import type {
  Provided,
  ProvidedByNodeDialog,
  ProvidedForFlowVariables,
} from "./types/provided";
import { getPossibleValuesFromUiSchema, hasAdvancedOptions } from "./utils";
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
    type: alert.type === "error" ? AlertType.ERROR : AlertType.WARN,
    details: alert.details,
  });
const {
  addStateProviderListener,
  callStateProviderListener,
  callStateProviderListenerByIndices,
} = useStateProviders();
const {
  registerWatcher: registerWatcherInternal,
  updateData: updateDataInternal,
  updateDataMultiplePaths: updateDataMultiplePathsInternal,
} = useGlobalWatchers();

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
    dependencies: dependencies.map((dep) => [dep]),
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

const trigger = (params: { id: string; indexIds?: string[] }) =>
  runWithDependencies(getTriggerCallback(params));
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

const callDataService = ({
  method,
  options,
}: Parameters<Provided["getData"]>[0]) =>
  jsonDataService?.data({ method, options })!;

const getPossibleValuesFromUiSchema2 = (control: Control) =>
  getPossibleValuesFromUiSchema(
    control,
    getChoices(callDataService),
    sendAlert,
  );

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

const onSettingsChanged = ({ data }: { data: SettingsData }) => {
  if (data) {
    setCurrentData(data);
    publishSettings();
  }
};

const applySettings = async () => {
  const { result } = await jsonDataService!.applyData(getData());
  if (result) {
    sendAlert({ message: result, type: AlertType.ERROR });
    return { isApplied: false };
  }
  return { isApplied: true };
};

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

const renderers = ref<null | NamedRenderer[]>(null);

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
  renderers.value = initializeRenderers({ hasNodeView, showAdvancedSettings });
  ready.value = true;
});

const dialogPopoverTeleportDest = ref<null | HTMLElement>(null);
const subPanelsTeleportDest = ref<null | HTMLElement>(null);

const provided: ProvidedByNodeDialog & ProvidedForFlowVariables = {
  isTriggerActive,
  registerWatcher,
  getData: jsonDataService.data.bind(jsonDataService),
  getPossibleValuesFromUiSchema: getPossibleValuesFromUiSchema2,
  flowVariablesApi: {
    getAvailableFlowVariables,
    getFlowVariableOverrideValue,
    clearControllingFlowVariable,
  },
  getPersistSchema,
  createArrayAtPath: (path: string) =>
    createArrayAtPath(getArrayIdsRecord(), path),
  getDialogPopoverTeleportDest: () => dialogPopoverTeleportDest.value,
  getPanelsContainer: () => subPanelsTeleportDest.value,
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
        @click="changeAdvancedSettings"
      >
        {{ showAdvancedSettings ? "Hide" : "Show" }} advanced settings
      </a>
    </template>
  </JsonFormsDialog>
  <div ref="subPanelsTeleportDest" />
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
