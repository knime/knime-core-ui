<script setup lang="ts">
import { computed } from "vue";

import hljs from "highlight.js";
import json from "highlight.js/lib/languages/json";
import "highlight.js/styles/stackoverflow-light.min.css";

hljs.registerLanguage("json", json);

const props = defineProps<{
  content: string;
}>();

const highlightedCode = computed(
  () => hljs.highlight(props.content, { language: "json" }).value,
);
</script>

<template>
  <div class="knime-cell-content">
    <pre><code v-html="highlightedCode" /></pre>
  </div>
</template>

<style scoped>
.knime-cell-content {
  max-width: fit-content;
}

pre {
  margin: 0;
  max-width: fit-content;
}
</style>
