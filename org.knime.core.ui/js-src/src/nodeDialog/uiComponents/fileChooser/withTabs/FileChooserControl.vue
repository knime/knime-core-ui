<script setup lang="ts">
import { composePaths } from "@jsonforms/core";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import { type FileChooserValue } from "../types/FileChooserProps";

import FileChooserControlBase from "./FileChooserControlBase.vue";

const props =
  defineProps<VueControlPropsForLabelContent<{ path: FileChooserValue }>>();

const changePath = (path: FileChooserValue) =>
  props.handleChange(composePaths(props.control.path, "path"), path);
</script>

<template>
  <FileChooserControlBase
    v-bind="props"
    :change-path
    :to-path="({ path }) => path"
  />
</template>
