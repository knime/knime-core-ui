<script setup lang="ts">
import { computed, onMounted } from "vue";

import { displayMode } from "../src/display-mode";

onMounted(async () => {});

const targetMode = computed(() =>
  displayMode.value === "small" ? "large" : "small",
);

const toggleMode = () => {
  displayMode.value = targetMode.value;
};
</script>

<template>
  <div class="debug-toolbar">
    <span class="debug-label">DEBUG:</span>
    <button @click="toggleMode">
      {{ targetMode }}
    </button>
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
</style>
