<!-- The component shown when resolving dynamic imports-->
<template>
  <div :class="['loading-dialog', 'skeleton-wrapper']">
    <div :class="['skeleton', 'loading-dialog-content']" />
  </div>
</template>

<style lang="postcss" scoped>
/* TODO UIEXT-2167: Move skeleton to common repo */
@import url("./skeleton.css");

.loading-dialog {
  margin-top: var(--space-16);
  height: 100%;
}

.loading-dialog-content {
  flex-grow: 1;
}
</style>
