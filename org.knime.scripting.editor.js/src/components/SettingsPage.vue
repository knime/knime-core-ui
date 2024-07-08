<script lang="ts">
import type { MenuItem } from "@knime/components";

export interface SettingsMenuItem extends MenuItem {
  showSettingsPage?: boolean;
}
</script>

<script setup lang="ts">
import { Button } from "@knime/components";
import ArrowLeft from "@knime/styles/img/icons/arrow-left.svg";

const emit = defineEmits<{
  (e: "close-settings-page"): void; // NOSONAR
}>();
</script>

<template>
  <div class="settings-title">
    <Button
      class="back-button"
      :with-border="false"
      @click="emit('close-settings-page')"
    >
      <div class="arrow-left"><ArrowLeft /></div>
    </Button>
    <h3><slot name="settings-title" /></h3>
  </div>
  <div class="settings-content">
    <slot name="settings-content" />
  </div>
</template>

<style scoped lang="postcss">
.settings-title {
  display: flex;
  flex-direction: row;
  justify-content: left;
  width: 100%;
  height: 120px;
  background-color: var(--knime-white);
  align-items: center;

  & .back-button {
    padding-right: var(--space-16);
  }

  & .arrow-left {
    border-right: 1px solid var(--knime-silver-sand);
  }

  & .button svg {
    height: 31px;
    width: 31px;
  }

  & h3 {
    font-size: 28px;
    margin-left: 0;
  }
}

.settings-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background-color: var(--knime-gray-ultra-light);
}
</style>
