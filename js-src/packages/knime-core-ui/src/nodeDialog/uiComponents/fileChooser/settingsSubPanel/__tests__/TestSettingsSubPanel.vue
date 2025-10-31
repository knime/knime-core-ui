<script lang="ts">
export const expandButtonId = "expandButton";
export const contentId = "content";
</script>

<script setup lang="ts">
import { ref } from "vue";

import { useApplyButton } from "..";
import type { Props as SettingsSubPanelProps } from "../SettingsSubPanelForFileChooser.vue";
import SettingsSubPanelForFileChooser from "../SettingsSubPanelForFileChooser.vue";

import TestSettingsSubPanelContent from "./TestSettingsSubPanelContent.vue";

export interface Props {
  settingsSubPanelConfig: SettingsSubPanelProps;
  onApply: () => Promise<void>;
}

defineProps<Props>();
const content = ref<null | typeof TestSettingsSubPanelContent>(null);

const getApplyButton: typeof useApplyButton = () => content.value?.applyButton;

defineExpose({
  getApplyButton,
});
</script>

<template>
  <SettingsSubPanelForFileChooser v-bind="settingsSubPanelConfig">
    <template #expand-button="{ expand }">
      <button :id="expandButtonId" @click="expand">Expand</button>
    </template>
    <template #default>
      <TestSettingsSubPanelContent
        :id="contentId"
        ref="content"
        :on-apply="onApply"
      />
    </template>
  </SettingsSubPanelForFileChooser>
</template>
