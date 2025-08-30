<script lang="ts">
export const GO_INTO_FOLDER_INJECTION_KEY = "goIntoFolderInjectionKey";
</script>

<script setup lang="ts">
import { ref, useTemplateRef } from "vue";

import { Button } from "@knime/components";
import { SettingsSubPanel } from "@knime/jsonforms";

import { setUpApplyButton } from "./useApplyButton";

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

const applyButtonTemplateRef = "applyButton";
const applyButton = useTemplateRef<HTMLElement>(applyButtonTemplateRef);
const {
  text: applyText,
  disabled: applyDisabled,
  onApply,
} = setUpApplyButton({ element: applyButton, initialValue: { text: "Apply" } });

const goIntoFolderButtonTemplateRef = "goIntoFolderButton";
const goIntoFolderButton = useTemplateRef<HTMLElement>(
  goIntoFolderButtonTemplateRef,
);
const {
  text: enterFolderButtonText,
  disabled: enterFolderButtonDisabled,
  shown: enterFolderButtonShown,
  onApply: onEnterFolderButtonClicked,
} = setUpApplyButton({
  key: GO_INTO_FOLDER_INJECTION_KEY,
  element: goIntoFolderButton,
  initialValue: {
    text: "Open folder",
  },
});

const apply = () =>
  onApply
    .value?.()
    .then(emitApplyAndClose)
    .catch(() => {});
</script>

<template>
  <SettingsSubPanel
    ref="settingsSubPanelRef"
    :show-back-arrow
    hide-buttons-when-expanded
  >
    <template #expand-button="{ expand }">
      <slot name="expand-button" :expand="expand" />
    </template>
    <template #default>
      <slot :apply-and-close="apply" />
    </template>
    <template #bottom-content>
      <div class="bottom-buttons">
        <Button with-border compact @click="close"> Cancel </Button>
        <div>
          <Button
            v-if="enterFolderButtonShown"
            :ref="goIntoFolderButtonTemplateRef"
            with-border
            compact
            :disabled="enterFolderButtonDisabled"
            @click="onEnterFolderButtonClicked"
          >
            {{ enterFolderButtonText }}
          </Button>
          <Button
            ref="applyButton"
            compact
            primary
            :disabled="applyDisabled"
            @click="apply"
          >
            {{ applyText }}
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

  & > div {
    display: flex;
    gap: var(--space-8);
    flex-direction: row;
  }
}
</style>
