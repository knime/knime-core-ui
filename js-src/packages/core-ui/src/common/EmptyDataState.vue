<script lang="ts" setup>
import { LoadingIcon } from "@knime/components";

const props = defineProps<{
  isDataLoaded: boolean;
  loadingAnimationEnabled: boolean;
}>();
</script>

<template>
  <div class="center">
    <h4 v-if="props.isDataLoaded">No data to display</h4>
    <span v-else-if="props.loadingAnimationEnabled">
      <LoadingIcon class="loading-icon" />
      Loading data
    </span>
  </div>
</template>

<style lang="postcss" scoped>
.center {
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  & h4 {
    color: rgb(70 70 70);
    font-size: 16px;
  }

  & span {
    font-size: 16px;
    text-align: center;
    font-style: italic;
    color: var(--knime-masala);

    & .loading-icon {
      width: 24px;
      height: 24px;
      stroke-width: 1px;
      margin: auto;
      stroke: var(--knime-masala);
      vertical-align: -6px;
      margin-right: 10px;
    }
  }
}
</style>
