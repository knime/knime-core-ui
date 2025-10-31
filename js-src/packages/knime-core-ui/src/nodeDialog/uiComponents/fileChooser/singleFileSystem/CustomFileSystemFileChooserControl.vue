<script setup lang="ts">
import { computed } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";
import { useProvidedState } from "@knime/jsonforms";

import { type FileChooserUiSchema } from "../../../types/FileChooserUiSchema";

import SingleFileSystemFileChooserControl from "./SingleFileSystemFileChooserControl.vue";

const props = defineProps<VueControlPropsForLabelContent<string>>();
const uischema = computed(() => props.control.uischema as FileChooserUiSchema);
const fileSystemId = useProvidedState(uischema, "fileSystemId");
</script>

<template>
  <SingleFileSystemFileChooserControl
    v-if="fileSystemId"
    v-bind="$props"
    :backend-type="fileSystemId"
  />
</template>
