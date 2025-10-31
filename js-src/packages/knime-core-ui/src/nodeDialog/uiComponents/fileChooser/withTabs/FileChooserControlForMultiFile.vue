<script setup lang="ts">
import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import type { MultiFileSelection } from "../types";

import FileChooserControlBase from "./FileChooserControlBase.vue";

const props =
  defineProps<VueControlPropsForLabelContent<MultiFileSelection["path"]>>();
</script>

<template>
  <FileChooserControlBase
    v-bind="props"
    :change-path="props.changeValue"
    :to-path="(path) => path"
  />
</template>
