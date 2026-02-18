<script setup lang="ts">
import { computed, inject, onMounted, ref } from "vue";

import {
  DialogService,
  JsonDataService,
  type SettingState,
  type UIExtensionService,
} from "@knime/ui-extension-service";

import TableCreatorDialogCore from "./TableCreatorDialogCore.vue";
import type { InitialData } from "./types";

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

const displayMode = ref<"small" | "large">("small");
const isLargeMode = computed(() => displayMode.value === "large");

const dialogInitialData = ref<InitialData | null>(null);

const settingState = ref<SettingState<number> | null>(null);
const numOperations = ref(0);

onMounted(async () => {
  const service = getKnimeService();
  const dialogService = new DialogService(service);
  displayMode.value = dialogService.getInitialDisplayMode();
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });

  const jsonDataService = new JsonDataService(service);
  const initialData = await jsonDataService.initialData();

  dialogInitialData.value = JSON.parse(
    initialData.settingsInitialData,
  ) as InitialData;

  settingState.value = dialogService.registerSettings("model")({
    initialValue: numOperations.value,
  });

  dialogService.setApplyListener(() =>
    jsonDataService.applyData({
      data: dialogInitialData.value?.data,
      flowVariableSettings: dialogInitialData.value?.flowVariableSettings,
    }),
  );
});

const incrementOperations = () => {
  numOperations.value++;
  settingState.value?.setValue(numOperations.value);
};
</script>

<template>
  <TableCreatorDialogCore
    v-if="dialogInitialData"
    :dialog-initial-data="dialogInitialData"
    :is-large-mode
    @adjusted="incrementOperations"
  />
</template>
