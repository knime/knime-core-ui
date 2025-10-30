<script setup lang="ts">
import { SkeletonItem } from "@knime/components";
</script>

<template>
  <div class="loading-container"><SkeletonItem /></div>
</template>

<style lang="postcss" scoped>
.loading-container {
  height: 100vh;
}
</style>
