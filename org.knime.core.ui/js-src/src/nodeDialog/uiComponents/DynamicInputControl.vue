<script setup lang="ts">
import { computed, watch } from "vue";
import { DispatchRenderer } from "@jsonforms/vue";

import { type VueControlProps, useProvidedState } from "@knime/jsonforms";

type DynamicInputUiSchema = {
  scope: string;
  options: {
    dynamicSettings?: {
      data: unknown;
      uiSchema: string;
      schema: string;
    };
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

const providedData = computed(() => dynamicSettings.value?.data ?? null);

watch(
  providedData,
  (newData) => {
    if (newData) {
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
