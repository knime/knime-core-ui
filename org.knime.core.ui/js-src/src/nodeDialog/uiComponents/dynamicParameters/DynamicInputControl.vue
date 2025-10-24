<script setup lang="ts">
import { computed, watch } from "vue";
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps, useProvidedState } from "@knime/jsonforms";

import type { PersistTreeSchema } from "@/nodeDialog/types/Persist";
import type { Update, UpdateResult } from "@/nodeDialog/types/Update";
import { hasAdvancedOptions } from "@/nodeDialog/utils";
import { addPersistSchemaForPath } from "@/nodeDialog/utils/paths";
import { clearRegisteredWatchersForSettingsId } from "../../composables/nodeDialog/useUpdates";
import inject from "../../utils/inject";

import {
  addSettingsIdStateProviders as addSettingsIdToStateProviders,
  addSettingsIdToTriggers,
} from "./composables";

type DynamicInputUiSchema = {
  scope: string;
  options: {
    dynamicSettings?: {
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
    );
  };
};

const props = defineProps<VueControlProps<object>>();
const uischema = computed(() => props.control.uischema as DynamicInputUiSchema);

const dynamicSettings = useProvidedState(uischema, "dynamicSettings");
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

watch(
  settingsId,
  (newSettingsId, previousSettingsId) => {
    if (newSettingsId === null && previousSettingsId) {
      clearRegisteredWatchersForSettingsId(previousSettingsId);
      return;
    }
    if (newSettingsId !== null) {
      if (providedUpdates.value !== null) {
        processUpdates?.({
          settingsIdContext: {
            settingsId: newSettingsId,
            currentParentPathSegments: props.control.path.split("."),
          },
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

addSettingsIdToStateProviders(settingsId);
addSettingsIdToTriggers(settingsId);

const providedData = computed(() => dynamicSettings.value?.data ?? null);

watch(
  providedData,
  (newData) => {
    if (
      newData &&
      JSON.stringify(newData) !== JSON.stringify(props.control.data)
    ) {
      props.changeValue(newData);
    }
  },
  { immediate: true },
);
</script>

<template>
  <DispatchRenderer
    v-if="dynamicSettings"
    :uischema="providedUiSchema"
    :schema="providedSchema"
    :path="props.control.path"
  />
</template>
