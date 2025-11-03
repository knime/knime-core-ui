<script setup lang="ts"></script>

<template>
  <div class="infinity-loading" />
</template>

<style scoped lang="postcss">
.infinity-loading {
  --loading-bar-background-color: var(--knime-silver-sand);
  --loading-bar-foreground-color: var(--knime-yellow);

  background-color: var(--loading-bar-background-color);
  height: 4px;
  width: 100%;
  position: relative;
  overflow: hidden;

  &::after {
    content: "";
    position: absolute;
    inset: 0;
    background-color: var(--loading-bar-foreground-color);
    z-index: 1;
    animation: loading 1.5s linear infinite;
  }
}

@keyframes loading {
  0% {
    left: -50%;
    right: 100%;
  }

  100% {
    left: 100%;
    right: -50%;
  }
}
</style>
