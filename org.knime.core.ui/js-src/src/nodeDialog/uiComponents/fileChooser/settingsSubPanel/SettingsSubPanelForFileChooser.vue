<script setup lang="ts">
import { ref } from "vue";

import { Button } from "@knime/components";
import { SettingsSubPanel } from "@knime/jsonforms";

import { setUpApplyButton } from ".";

export interface Props {
  showBackArrow?: boolean;
}

withDefaults(defineProps<Props>(), {
  showBackArrow: false,
});

const emit = defineEmits(["apply"]);

const settingsSubPanelRef = ref<InstanceType<typeof SettingsSubPanel>>();

const close = () => {
  settingsSubPanelRef.value?.close();
};
const emitApplyAndClose = () => {
  emit("apply");
  close();
};

const {
  text: applyText,
  disabled: applyDisabled,
  element: applyButton,
  onApply,
} = setUpApplyButton();

const {
  text: otherApplyText,
  disabled: otherApplyDisabled,
  element: goIntoFolderButton,
  onApply: otherOnApply,
} = setUpApplyButton("goIntoSelectedFolder");
otherApplyText.value = "Open folder";

const apply = () =>
  onApply
    .value?.()
    .then(emitApplyAndClose)
    .catch(() => {});
</script>

<template>
  <SettingsSubPanel ref="settingsSubPanelRef" :show-back-arrow="showBackArrow">
    <template #expand-button="{ expand }">
      <slot name="expand-button" :expand="expand" />
    </template>
    <template #default>
      <slot />
    </template>
    <template #bottom-content>
      <div class="bottom-buttons">
        <Button with-border compact @click="close"> Cancel </Button>
        <div>
          <Button
            ref="applyButton"
            compact
            primary
            :disabled="applyDisabled"
            @click="apply"
          >
            {{ applyText }}
          </Button>
          <Button
            ref="goIntoFolderButton"
            compact
            :disabled="otherApplyDisabled"
            @click="otherOnApply"
          >
            {{ otherApplyText }}
          </Button>
        </div>
      </div>
    </template>
  </SettingsSubPanel>
</template>

<style scoped lang="postcss">
.bottom-buttons {
  display: flex;
  justify-content: space-between;
  height: 60px;
  padding: 14px 20px;
}
</style>
