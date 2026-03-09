<script setup lang="ts">
import { ref, watch } from "vue";

import { useKdsDarkMode, useKdsLegacyMode } from "@knime/kds-components";

import { displayMode } from "../src/display-mode";

const { legacyMode } = useKdsLegacyMode(true);
const { currentMode } = useKdsDarkMode();

type KdsMode = "legacy" | "light" | "dark" | "system";
const kdsMode = ref<KdsMode>("legacy");

watch(kdsMode, (mode) => {
  switch (mode) {
    case "legacy":
      legacyMode.value = true;
      currentMode.value = "light";
      break;
    case "light":
      legacyMode.value = false;
      currentMode.value = "light";
      break;
    case "dark":
      legacyMode.value = false;
      currentMode.value = "dark";
      break;
    case "system":
      legacyMode.value = false;
      currentMode.value = "system";
      break;
  }
});
</script>

<template>
  <div class="debug-toolbar">
    <span class="debug-label">DEBUG:</span>
    Display Mode:
    <label>
      <input v-model="displayMode" type="radio" value="small" />
      Small
    </label>
    <label>
      <input v-model="displayMode" type="radio" value="large" />
      Large
    </label>
    <span>|</span>
    KDS Mode:
    <label>
      <input v-model="kdsMode" type="radio" value="legacy" />
      Legacy
    </label>
    <label>
      <input v-model="kdsMode" type="radio" value="light" />
      Light
    </label>
    <label>
      <input v-model="kdsMode" type="radio" value="dark" />
      Dark
    </label>
    <label>
      <input v-model="kdsMode" type="radio" value="system" />
      System
    </label>
  </div>
</template>

<style scoped>
.debug-toolbar {
  font-size: 10px;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: red;
  color: white;
  padding: 4px 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.debug-label {
  font-weight: bold;
}

label {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
