<script setup lang="ts">
import { FunctionButton } from "@knime/components";
import FolderLenseIcon from "@knime/styles/img/icons/folder-lense.svg";

import SettingsSubPanelForFileChooser from "./settingsSubPanel/SettingsSubPanelForFileChooser.vue";

interface Props {
  disabled: boolean;
}

defineProps<Props>();
const emit = defineEmits(["apply"]);
</script>

<template>
  <SettingsSubPanelForFileChooser @apply="() => emit('apply')">
    <template #expand-button="{ expand }">
      <FunctionButton
        class="fit-content"
        :disabled="disabled"
        primary
        compact
        @click="expand"
        ><FolderLenseIcon
      /></FunctionButton>
    </template>
    <template #default="{ applyAndClose }">
      <slot :apply-and-close />
    </template>
  </SettingsSubPanelForFileChooser>
</template>
