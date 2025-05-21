<script setup lang="ts">
import { LoadingIcon } from "@knime/components";

defineProps<{
  errorMessage: string | null;
  isLoading: boolean;
}>();
</script>

<template>
  <div class="error-message">
    <span v-if="isLoading" class="loading-icon">
      <LoadingIcon />
    </span>
    <span v-if="isLoading" class="loading-text">Validating...</span>
    <span v-else-if="errorMessage !== null" class="error-text">
      {{ errorMessage }}
    </span>
  </div>
</template>

<style lang="postcss" scoped>
.error-message {
  font-size: 10px;
  min-height: 13px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-8);

  & .error-text {
    color: var(--knime-coral);
  }

  & .loading-text {
    color: var(--knime-dove-gray);
    font-style: italic;
  }

  & .loading-icon {
    width: 12px;
    height: 12px;
  }
}
</style>
