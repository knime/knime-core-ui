<script setup lang="ts">
import CircleHelpIcon from "@knime/styles/img/icons/circle-help.svg";
</script>

<template>
  <CircleHelpIcon class="missing-value-icon" />
</template>

<style scoped>
.missing-value-icon {
  width: 14px;
  stroke-width: calc(32px / 14);
  stroke: var(--theme-color-error);
}
</style>
