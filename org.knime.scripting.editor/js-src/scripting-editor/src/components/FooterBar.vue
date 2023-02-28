<script>
// https://bitbucket.org/KNIME/knime-core-ui/src/master/org.knime.core.ui/js-src/src/components/NodeDialog.vue
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

export default defineComponent({ name: 'FooterBar',
    components: {
        Button,
    } });
</script>

<template>
  <div class="container">
    <Button
      class="button"
      compact
      with-border
    >
      Cancel
    </Button>
    <Button
      class="button"
      primary
      compact
    >
      Ok
    </Button>
  </div>
</template>

<style lang="postcss" scoped>
.container {
  display: flex;
  justify-content: space-between;
  height: var(--controls-height);
  padding: 13px 20px 5px;
  background-color: var(--knime-gray-light-semi);
  border-top: 1px solid var(--knime-silver-sand);
}
</style>
