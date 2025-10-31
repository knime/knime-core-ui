<script setup lang="ts">
import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import SingleFileSystemFileChooserControl from "./SingleFileSystemFileChooserControl.vue";

defineProps<VueControlPropsForLabelContent<string>>();
</script>

<template>
  <SingleFileSystemFileChooserControl v-bind="$props" backend-type="local" />
</template>
