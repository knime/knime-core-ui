<script setup lang="ts">
defineProps<{ elements: object[] }>();
</script>

<template>
  <div class="vertical-layout">
    <template v-for="(element, index) in elements" :key="index">
      <slot :element="element" :index="index" />
    </template>
  </div>
</template>

<style scoped lang="postcss">
.vertical-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;

  --vertical-margin: 11px;

  margin-bottom: var(--vertical-margin);

  /* TODO: UIEXT-1061 workaround to make the last dialog element fill the remaining height, used in RichTextInput */

  &:last-child {
    flex: 1;
  }

  /* if a dialog starts with a section header we don't need extra top margin, otherwise adding it here */
  &:not(:has(:first-child > .section:first-child)) {
    margin-top: var(--vertical-margin);
  }
}
</style>
