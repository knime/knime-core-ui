<script setup lang="ts">
import { LoadingIcon } from "@knime/components";
</script>

<template>
  <div class="info">
    <span>Loading...</span>
    <LoadingIcon class="icon" />
  </div>
</template>

<style scoped lang="postcss">
.info {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 10px;

  & .icon {
    height: 14px;
  }
}
</style>
