<script setup lang="ts">
import { Description } from "@knime/components";
import { KdsInfoToggleButton } from "@knime/kds-components";

import type { DescriptionPopoverProps } from "./types/DescriptionPopoverProps";

withDefaults(defineProps<DescriptionPopoverProps>(), {
  hover: false,
  ignoredClickOutsideTarget: null,
});
</script>

<template>
  <KdsInfoToggleButton :hidden="!hover">
    <div class="description-wrapper">
      <Description class="description" :text="html" render-as-html />
    </div>
  </KdsInfoToggleButton>
</template>

<style scoped>
/** A deep selector is necessary, since Description is a multi-root component
* (see https://github.com/vuejs/core/issues/5446)
*/
.description-wrapper :deep(.description) {
  max-height: 300px;
  overflow: auto;
  pointer-events: auto;
  font-size: 13px;
  font-weight: 300;
  line-height: 18.78px;

  /* Description component line-height-to-font-size-ratio of 26/18 times font size of 13 */
  color: var(--knime-masala);
}
</style>
