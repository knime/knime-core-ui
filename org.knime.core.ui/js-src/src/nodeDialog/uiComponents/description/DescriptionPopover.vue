<script setup lang="ts">
import { Description } from "@knime/components";
import DescriptionIcon from "@knime/styles/img/icons/circle-help.svg";

import DialogPopover from "../../popover/DialogPopover.vue";

import type { DescriptionPopoverProps } from "./types/DescriptionPopoverProps";

withDefaults(defineProps<DescriptionPopoverProps>(), {
  hover: false,
  ignoredClickOutsideTarget: null,
});
</script>

<template>
  <DialogPopover
    tooltip="Click for more information"
    popover-width="max-content"
    :ignored-click-outside-target="ignoredClickOutsideTarget"
  >
    <template #icon="{ expanded, focused }">
      <DescriptionIcon v-show="hover || expanded || focused" />
    </template>
    <template #popover>
      <div class="description-wrapper">
        <Description class="description" :text="html" render-as-html />
      </div>
    </template>
  </DialogPopover>
</template>

<style lang="postcss" scoped>
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
