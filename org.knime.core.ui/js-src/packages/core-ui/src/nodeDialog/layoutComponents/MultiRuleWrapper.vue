<script setup lang="ts">
import { DispatchRenderer } from "@jsonforms/vue";

import type { VueLayoutProps } from "@knime/jsonforms";

defineProps<VueLayoutProps>();
</script>

<template>
  <template v-for="(element, index) in layout.uischema.elements" :key="index">
    <DispatchRenderer
      :schema="layout.schema"
      :uischema="element"
      :path="layout.path"
      :enabled="layout.enabled"
      :renderers="layout.renderers"
      :cells="layout.cells"
    />
  </template>
</template>
