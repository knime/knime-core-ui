<script lang="ts">
import Button from "webapps-common/ui/components/Button.vue";

// TODO: Remove this component when the UI Extensions provide the footer bar

import { defineComponent } from "vue";

export default defineComponent({
  name: "FooterBar",
  components: {
    Button,
  },
  emits: ["scriptingEditorCancelled", "scriptingEditorOkayed"],
});
</script>

<template>
  <div class="container">
    <Button
      class="button"
      compact
      with-border
      @click="$emit('scriptingEditorCancelled')"
    >
      Cancel
    </Button>
    <Button
      class="button"
      primary
      compact
      @click="$emit('scriptingEditorOkayed')"
    >
      Ok
    </Button>
  </div>
</template>

<style lang="postcss" scoped>
.container {
  --controls-height: 49px;

  display: flex;
  justify-content: space-between;
  height: var(--controls-height);
  padding: 9px 20px;
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
}
</style>
