<script setup lang="ts">
import { computed, inject, onMounted, ref } from "vue";

import {
  DialogService,
  type UIExtensionService,
} from "@knime/ui-extension-service";

const getKnimeService = inject<() => UIExtensionService>("getKnimeService")!;

// Display mode tracking
const displayMode = ref<"small" | "large">("small");
const isLargeMode = computed(() => displayMode.value === "large");

onMounted(() => {
  const service = getKnimeService();
  const dialogService = new DialogService(service);
  displayMode.value = dialogService.getInitialDisplayMode();
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    displayMode.value = mode;
  });
});
</script>

<template>
  <div class="table-creator-dialog">
    <h1>Table Creator Dialog</h1>
    <p>Current mode: {{ displayMode }}</p>

    <!-- Content shown in all modes -->
    <div class="always-visible">
      <p>This content is always visible</p>
    </div>

    <!-- Content shown only in large mode -->
    <div v-if="isLargeMode" class="large-mode-only">
      <h2>Large Mode Features</h2>
      <p>This content only appears when the dialog is enlarged!</p>
      <ul>
        <li>Extra controls</li>
        <li>Advanced options</li>
        <li>More detailed information</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.table-creator-dialog {
  padding: 20px;
}

.always-visible {
  padding: 10px;
  margin-bottom: 20px;
  border-radius: 4px;
}

.large-mode-only {
  padding: 15px;
  border-radius: 4px;
  margin-top: 20px;
}

.large-mode-only h2 {
  margin-top: 0;
}
</style>
