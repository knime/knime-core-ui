<script lang="ts">
/**
 * Duplicated from @knime/jsonforms since we will introduce error messages as part of the
 * input fields themselves anyway in the future.
 *
 * Not easy to reuse the component from @knime/jsonforms since we would need to import the
 * css for all jsonforms renderers as well.
 */
export default {};
</script>

<script setup lang="ts">
import ErrorLine from "./ErrorLine.vue";

defineProps<{
  errors: Array<string>;
}>();
</script>

<template>
  <div :class="['error-message-wrapper', { 'with-error': errors.length > 0 }]">
    <slot />
    <ErrorLine
      v-for="(error, index) in errors"
      :key="index"
      :error
      class="error-message"
    />
  </div>
</template>

<style lang="postcss" scoped>
.error-message-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--error-message-vertical-padding);

  &.with-error {
    /**
     * We want to not take any additional space if the error consists of one line.
     */
    margin-bottom: calc(
      -1 * (var(--error-message-single-line-height) +
            var(--error-message-vertical-padding))
    );
  }

  & .error-message {
    flex-shrink: 0;
  }
}
</style>
