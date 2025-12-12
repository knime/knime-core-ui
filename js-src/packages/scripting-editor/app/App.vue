<script setup lang="ts">
import { ScriptingEditor } from "../lib/main";

import { getAppInitialData } from "./app-initial-data";

const initialData = getAppInitialData();
</script>

<template>
  <main>
    <ScriptingEditor
      :language="initialData.language"
      :file-name="initialData.fileName"
      :initial-pane-sizes="{
        bottom: 0,
        left: 0,
        right: 100,
      }"
    />
  </main>
</template>

<style>
@import url("@knime/styles/css");
</style>
