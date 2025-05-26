<script setup lang="ts">
import { DispatchRenderer } from "@jsonforms/vue";

import {
  LabeledControl,
  VerticalLayoutBase,
  type VueLayoutProps,
} from "@knime/jsonforms";

defineProps<VueLayoutProps>();
</script>

<template>
  <LabeledControl :label="layout.label" large>
    <template #buttons="{ hover }">
      <slot name="buttons" :hover="hover" />
    </template>
    <template #default>
      <VerticalLayoutBase
        #default="{ element, index }"
        class="labeled-group-content"
        :elements="layout.uischema.elements"
      >
        <DispatchRenderer
          :key="`${layout.path}-${index}`"
          :schema="layout.schema"
          :uischema="element"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </VerticalLayoutBase>
    </template>
  </LabeledControl>
</template>

<style scoped lang="postcss">
.labeled-group-content {
  padding-bottom: 8px;
  padding-top: 0 !important;
}
</style>
