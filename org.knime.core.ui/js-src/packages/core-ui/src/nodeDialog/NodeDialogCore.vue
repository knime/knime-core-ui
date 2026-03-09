<script setup lang="ts">
import { computed, nextTick, onMounted, provide, reactive, ref } from "vue";
import { type JsonSchema, type UISchemaElement } from "@jsonforms/core";
import { cloneDeep, set } from "lodash-es";

import {
  type AlertParams as JsonFormsAlertParams,
  JsonFormsDialog,
  type NamedRenderer,
} from "@knime/jsonforms";
import type {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars, unused-imports/no-unused-imports
  AlertingService,
  DialogService,
} from "@knime/ui-extension-service";

import * as flowVariablesApi from "./api/flowVariables";
import type { FlowSettings } from "./api/types";
import type { NodeDialogCoreRpcMethods } from "./api/types/RpcTypes";
import { getFlowVariablesMap } from "./composables/components/useProvidedFlowVariablesMap";
import {
  type ArrayRecord,
  createArrayAtPath,
} from "./composables/nodeDialog/useArrayIds";
import useCurrentData from "./composables/nodeDialog/useCurrentData";
import { provideAndGetSetupMethodForDirtySettings } from "./composables/nodeDialog/useDirtySettings";
import useGlobalWatchers from "./composables/nodeDialog/useGlobalWatchers";
import useStateProviders from "./composables/nodeDialog/useStateProviders";
import useTriggers, {
  type TriggerCallback,
} from "./composables/nodeDialog/useTriggers";
import useUpdates, {
  type SettingsIdContext,
} from "./composables/nodeDialog/useUpdates";
import { initializeRenderers } from "./renderers";
import type { NodeDialogCoreInitialData } from "./types/InitialData";
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

export interface NodeDialogCoreProps {
  /**
   * The initial data as they can be retrieved from a NodeParameters class in the backend.
   * The data and flow variables map are NOT updated in place.
   *
   * Since the flowVariablesMap can also be used for settings outside of this component,
   * they are to provided separately via the `useFlowVariableSystem` composable.
   * Set the initial flow variables map there via `setInitialFlowVariablesMap`.
   *
   * To retrieve the current data at any point, use the exposed method `getCurrentData()`.
   */
  initialData: NodeDialogCoreInitialData;
  /**
   * (see {@link DialogService.hasNodeView}).
   */
  hasNodeView: boolean;
  /**
   * RPC call method to communicate with backend. The method is only called on demand,
   * i.e., e.g. it is not necessary to provide the file chooser API if no file chooser
   * is part of the used renderers.
   *
   * Note that not all methods that can be called from within the NodeDialogCore are listed
   * in the type {@link NodeDialogCoreRpcMethods}; For example `settings.initializeButton`
   * related to the legacy ButtonControl is not part of it.
   */
  callRpcMethod: NodeDialogCoreRpcMethods;
  /**
   * Function to apply the dialog settings.
   *
   * @param dataTransformer A function that optionally transforms the data-part
   * (i.e. model- or view-data) of the settings before applying them
   */
  callApplyData?: (dataTransformer: (data: any) => void) => Promise<void>;
  /**
   * Pass down {@link DialogService.registerSettings} here to automatically handle dirtiness.
   * For that it is necessary that the used data are nested inside a "view" or a "model" property
   */
  registerSettings: DialogService["registerSettings"];
}

const props = defineProps<NodeDialogCoreProps>();

// Emits
export interface NodeDialogCoreEmits {
  /**
   * Emits the current data whenever they change.
   * Can be used to communicate view settings to a view.
   */
  (e: "publishData", data: SettingsData): void;
  /**
   * See {@link AlertingService.sendAlert}
   */
  (
    e: "alert",
    alert: { message: string; type: "error" | "warn"; details?: string },
  ): void;
  /**
   * Used in the default NodeDialog to hide knime-ui controls when subpanels are expanded.
   * It does not hurt to not handle this event.
   */
  (e: "set-controls-visibility", visible: boolean): void;
}

const emit = defineEmits<NodeDialogCoreEmits>();

const { setCurrentData, setInitialData, getCurrentData, getInitialValue } =
  useCurrentData();

const flowVariablesMap = getFlowVariablesMap();
const getDataAndFlowVariableSettings = () => ({
  data: getCurrentData(),
  flowVariableSettings: flowVariablesMap,
});

const publishSettings = () => emit("publishData", getCurrentData());

const sendAlert = (alert: {
  message: string;
  type: "error" | "warn";
  details?: string;
}) => emit("alert", alert);

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

// RPC call wrapper
const callDataService = props.callRpcMethod;

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

const runWithDependencies = async (
  triggerCallback: ReturnType<TriggerCallback>,
) => (await triggerCallback(getCurrentData()))(getCurrentData());

const trigger = (params: unknown) =>
  runWithDependencies(
    getTriggerCallback(params as { id: string; indexIds?: string[] }),
  );

const isTriggerActive = (params: { id: string; indexIds?: string[] }) =>
  getTriggerIsActiveCallback(params)(getCurrentData());

const persistSchema = ref<PersistSchema | null>(null);
const getPersistSchema = () => persistSchema.value ?? {};
// UPDATES
const pathIsControlledByFlowVariable = (path: string) =>
  getConfigPaths({ path, persistSchema: getPersistSchema() })
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
  setValueAtPath: async (path: string, data: any) => {
    if (jsonforms.value) {
      await jsonforms.value?.updateData?.(path, data);
    } else {
      set(getCurrentData(), path, data);
      publishSettings();
    }
  },
  globalArrayIdsRecord,
  callDataService,
});

const resolveInitialUpdates = (
  initialUpdates: UpdateResult[],
  settingsIdContext?: SettingsIdContext,
) =>
  resolveUpdateResults(initialUpdates, getCurrentData(), [], settingsIdContext);

const registerGlobalUpdates = (
  globalUpdates: Update[],
  settingsIdContext?: SettingsIdContext,
) => {
  const initialTransformation = registerUpdates(
    globalUpdates,
    settingsIdContext,
  );
  if (initialTransformation) {
    // we need to wait for the next tick to ensure that array items are already rendered and have an _id
    nextTick(() => {
      runWithDependencies(initialTransformation);
    });
  }
};

const processUpdates = ({
  settingsIdContext,
  updates,
}: {
  updates: {
    globalUpdates?: Update[];
    initialUpdates?: UpdateResult[];
  };
  settingsIdContext?: SettingsIdContext;
}) => {
  resolveInitialUpdates(updates.initialUpdates ?? [], settingsIdContext);
  registerGlobalUpdates(updates.globalUpdates ?? [], settingsIdContext);
};

// Dirty settings
const { setRegisterSettingsMethod } =
  provideAndGetSetupMethodForDirtySettings();

const setSubPanelExpanded = ({ isExpanded }: { isExpanded: boolean }) => {
  emit("set-controls-visibility", !isExpanded);
};

const showAdvancedSettings = ref(false);

const flawedControllingVariablePaths = ref(new Set<string>());
const possiblyFlawedControllingVariablePaths = ref(new Set<string>());
const schema = ref<JsonSchema | null>(null);
const uischema = ref<UISchemaElement | null>(null);

const getAvailableFlowVariables = (persistPath: string) =>
  flowVariablesApi.getAvailableFlowVariables(
    callDataService,
    persistPath,
    getDataAndFlowVariableSettings(),
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
  const { data, flowVariableSettings } = cloneDeep(
    getDataAndFlowVariableSettings(),
  );
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
  return (receivedData as any).result || null;
};

const performCustomValidation = async (
  id: string,
  value: any,
): Promise<string | null> => {
  const receivedData = await callDataService({
    method: "settings.performCustomValidation",
    options: [id, value],
  });
  return (receivedData as any).result || null;
};

const onSettingsChanged = ({ data }: { data: unknown }) => {
  if (data) {
    setCurrentData(data as SettingsData);
    publishSettings();
  }
};

const changeAdvancedSettings = () => {
  showAdvancedSettings.value = !showAdvancedSettings.value;
};

const pathsOfDynamicParametersWithAdvancedOptions = reactive(new Set());

const setAdvancedDynamicParameters = (path: string, hasAdvanced: boolean) => {
  if (hasAdvanced) {
    pathsOfDynamicParametersWithAdvancedOptions.add(path);
  } else {
    pathsOfDynamicParametersWithAdvancedOptions.delete(path);
  }
};

const showAdvancedSettingsButton = computed(() => {
  if (!uischema.value) {
    return false;
  }
  if (pathsOfDynamicParametersWithAdvancedOptions.size > 0) {
    return true;
  }
  return hasAdvancedOptions(uischema.value);
});

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

const ready = ref(false);
onMounted(() => {
  schema.value = props.initialData.schema;
  uischema.value = props.initialData.ui_schema;
  setInitialData(cloneDeep(props.initialData.data));
  initializeFlowVariablesMap({ flowVariableSettings: flowVariablesMap });
  setCurrentData(props.initialData.data);
  setRegisterSettingsMethod(props.registerSettings);
  persistSchema.value = props.initialData.persist;
  registerGlobalUpdates(props.initialData.globalUpdates ?? []);
  resolveInitialUpdates(props.initialData.initialUpdates ?? []);
  renderers.value = initializeRenderers({
    hasNodeView: props.hasNodeView,
    showAdvancedSettings,
    performExternalValidation,
  });
  ready.value = true;
});

const dialogPopoverTeleportDest = ref<null | HTMLElement>(null);

const provided: ProvidedByNodeDialog & ProvidedForFlowVariables = {
  isTriggerActive,
  registerWatcher,
  getData: callDataService,
  applyData: props.callApplyData,
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
  getInitialValue,
  processUpdates,
  setAdvancedDynamicParameters,
};

Object.entries(provided).forEach(([key, value]) => {
  provide(key, value);
});

// exposed for tests:
defineExpose({
  ...provided,
  setCurrentData,
  getCurrentData,
  sendAlert,
  schema,
  getAvailableFlowVariables,
  getFlowVariableOverrideValue,
  trigger,
  addStateProviderListener,
  flawedControllingVariablePaths,
  possiblyFlawedControllingVariablePaths,
  getDataAndFlowVariableSettings,
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
    :validate="performCustomValidation"
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
        v-if="showAdvancedSettingsButton"
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
