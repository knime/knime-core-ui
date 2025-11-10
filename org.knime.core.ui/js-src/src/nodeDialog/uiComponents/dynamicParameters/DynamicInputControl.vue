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

type DynamicInputUiSchema = {
  scope: string;
  options: {
    dynamicSettings?:
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
      | null
      /* initial value */
      | {
          data?: unknown;
          uiSchema?: string;
          schema?: string;
          updates?: null;
          persist?: null;
          settingsId?: null;
        };
  };
};

const props = defineProps<VueControlProps<object | null>>();
const uischema = computed(() => props.control.uischema as DynamicInputUiSchema);

const dynamicSettings = useProvidedState(
  uischema,
  "dynamicSettings",
  {} as DynamicInputUiSchema["options"]["dynamicSettings"],
);
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

/**
 * No immediate watch since we don't want the value to be set to null on initial load just to be updated once the first
 * dynamic settings arrive. This is why we use {} as a default value for the dynamicSettings state which is a state that
 * can not be provided (unlike null) and thus the first update will trigger this watcher.
 */
watch(dynamicSettings, (newDynamicSettings) => {
  const newData = newDynamicSettings?.data ?? null;
  if (JSON.stringify(newData) !== JSON.stringify(props.control.data)) {
    props.changeValue(newData);
  }
});
</script>

<template>
  <DispatchRenderer
    v-if="dynamicSettings?.uiSchema"
    :uischema="providedUiSchema"
    :schema="providedSchema"
    :path="props.control.path"
  />
</template>
