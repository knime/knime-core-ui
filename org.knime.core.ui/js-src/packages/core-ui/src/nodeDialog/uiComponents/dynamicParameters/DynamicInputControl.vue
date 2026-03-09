<script lang="ts">
export const DYNAMIC_SETTINGS_KEY = "dynamicSettings";
export type DynamicSettings =
  | ({
      data: unknown;
      uiSchema: string;
      schema: string;
    } & (
      | {
          updates: null;
          persist: null;
          settingsId: null;
        }
      | {
          updates: string;
          persist: string;
          settingsId: string;
        }
    ))
  | null;

type DynamicInputUiSchema = {
  scope: string;
  options: {
    dynamicSettings?: DynamicSettings;
  };
};
</script>

<script setup lang="ts">
import { computed, watch } from "vue";
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps, useProvidedState } from "@knime/jsonforms";

import { clearRegisteredWatchersForSettingsId } from "@/nodeDialog/composables/nodeDialog/useUpdates";
import type { PersistTreeSchema } from "@/nodeDialog/types/Persist";
import type { Update, UpdateResult } from "@/nodeDialog/types/Update";
import { hasAdvancedOptions } from "@/nodeDialog/utils";
import inject from "@/nodeDialog/utils/inject";
import { addPersistSchemaForPath } from "@/nodeDialog/utils/paths";

import {
  addSettingsIdStateProviders as addSettingsIdToStateProviders,
  addSettingsIdToTriggers,
} from "./composables";

const props = defineProps<VueControlProps<object | null>>();
const uischema = computed(() => props.control.uischema as DynamicInputUiSchema);

const dynamicSettings = useProvidedState(uischema, DYNAMIC_SETTINGS_KEY);
const providedUiSchema = computed(() =>
  JSON.parse(dynamicSettings.value?.uiSchema || "{}"),
);
const providedSchema = computed(() =>
  JSON.parse(dynamicSettings.value?.schema || "{}"),
);

// Register scoped updates if we have them
const settingsId = computed(() => dynamicSettings.value?.settingsId ?? null);
const providedUpdates = computed(() => {
  const updatesStr = dynamicSettings.value?.updates;
  if (updatesStr) {
    try {
      return JSON.parse(updatesStr) as {
        globalUpdates?: Update[];
        initialUpdates?: UpdateResult[];
      };
    } catch {
      return null;
    }
  }
  return null;
});

const providedPersistSchema = computed(() => {
  const persistStr = dynamicSettings.value?.persist;
  if (persistStr) {
    try {
      return JSON.parse(persistStr) as PersistTreeSchema;
    } catch {
      return null;
    }
  }
  return null;
});

const processUpdates = inject("processUpdates");
const setAdvancedDynamicParameters = inject("setAdvancedDynamicParameters"); // path: string, hasAdvanced: boolean) => void;

watch(
  providedUiSchema,
  (newUiSchema) => {
    setAdvancedDynamicParameters?.(
      props.control.path,
      hasAdvancedOptions(newUiSchema),
    );
  },
  { immediate: true },
);

const settingsIdContext = computed(() => {
  const id = settingsId.value;
  if (id === null) {
    return null;
  }
  return {
    settingsId: id,
    currentParentPathSegments: props.control.path.split("."),
  };
});

watch(
  settingsId,
  (_newSettingsId, previousSettingsId) => {
    if (previousSettingsId) {
      clearRegisteredWatchersForSettingsId(previousSettingsId);
      return;
    }
    if (settingsIdContext.value !== null) {
      if (providedUpdates.value !== null) {
        processUpdates?.({
          settingsIdContext: settingsIdContext.value,
          updates: providedUpdates.value,
        });
      }
      if (providedPersistSchema.value !== null) {
        addPersistSchemaForPath(
          props.control.path,
          providedPersistSchema.value,
        );
      }
      setAdvancedDynamicParameters?.(
        props.control.path,
        hasAdvancedOptions(providedUiSchema.value),
      );
    }
  },
  { immediate: true },
);

addSettingsIdToStateProviders(settingsIdContext);
addSettingsIdToTriggers(settingsIdContext);
</script>

<template>
  <DispatchRenderer
    v-if="dynamicSettings?.uiSchema"
    :uischema="providedUiSchema"
    :schema="providedSchema"
    :path="props.control.path"
  />
</template>
