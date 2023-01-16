<script>
//
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

export default defineComponent({ name: 'FooterBar',
    components: {
        Button
    } });
</script>

<template>
  <div class="container">
    <Button
      class="button"
      primary
      with-border
      compact
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
.container{
  width: 100%;
  display: flex;
  justify-content: space-between;
  background-color: var(--knime-black);
  border-top: 3px solid var(--knime-black);
  border-bottom: 2px solid var(--knime-black);
  padding: 2px;
}

.button{
  flex: 0 1;
  width: auto;
  justify-content: center;
  display: flex;
  background-color: var(--knime-black);
  border-bottom: 1px solid var(--knime-black);
 
}
</style>
