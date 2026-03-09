<script setup lang="ts">
import { computed } from "vue";
import { rendererProps, useJsonFormsRenderer } from "@jsonforms/vue";

import { InlineMessage } from "@knime/components";

const props = defineProps(rendererProps());
const { renderer } = useJsonFormsRenderer(props);
const nodeName = computed<string>(
  () => renderer.value.uischema.options?.nodeName,
);
const title = computed(() => `Not Supported: ${nodeName.value}`);
const description = computed(
  () => `The node "${nodeName.value}" is not supported in this dialog.`,
);
</script>

<template>
  <InlineMessage :title :description variant="error" />
</template>
