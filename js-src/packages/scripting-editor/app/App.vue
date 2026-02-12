<script setup lang="ts">
import { ref } from "vue";

import {
  ScriptingEditor,
  getSettingsService,
  joinSettings,
  useStaticCompletionProvider,
} from "../lib/main";
import { NodeParametersPanel } from "../lib/parameters";

import { getAppInitialData } from "./app-initial-data";

const initialData = getAppInitialData();

// Settings

const nodeParametersPanel = ref<InstanceType<
  typeof NodeParametersPanel
> | null>(null);

const settingsInitialData = getSettingsService().getSettingsInitialData()!;
const toSettings = (commonSettings: { script: string }) =>
  joinSettings(
    commonSettings,
    nodeParametersPanel.value?.getDataAndFlowVariableSettings(),
  );
const hasAdditionalNodeParameters =
  "elements" in settingsInitialData.ui_schema &&
  Array.isArray(settingsInitialData.ui_schema.elements) &&
  settingsInitialData.ui_schema.elements.length > 0;

// Register static completion items if provided
if (initialData.staticCompletionItems?.length) {
  useStaticCompletionProvider(
    initialData.language,
    initialData.staticCompletionItems,
  );
}
</script>

<template>
  <main>
    <ScriptingEditor
      :language="initialData.language"
      :file-name="initialData.fileName"
      :to-settings="toSettings"
      :initial-pane-sizes="{
        bottom: 0,
        left: 280,
        right: 380,
      }"
      :show-control-bar="false"
    >
      <template v-if="hasAdditionalNodeParameters" #right-pane>
        <NodeParametersPanel ref="nodeParametersPanel" />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("@knime/styles/css");
@import url("@knime/kds-styles/kds-variables.css");
@import url("@knime/kds-styles/kds-legacy-theme.css");
</style>
