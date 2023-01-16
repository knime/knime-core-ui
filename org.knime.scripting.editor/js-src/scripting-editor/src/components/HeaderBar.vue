<script>
// TODO: emit run

import PlayIcon from 'webapps-common/ui/assets/img/icons/play.svg';
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

export default defineComponent({ name: 'HeaderBar',
    components: {
        Button,
        PlayIcon
    } });
</script>

<template>
  <div class="container">
    <Button>
      <PlayIcon class="play" />
    </Button>
  </div>
</template>


<style lang="postcss" scoped>


.container{
  height: var(--app-toolbar-height);
  background-color: var(--knime-porcelain);
  border-bottom: 2px solid var(--knime-silver-sand);
  width: 100%;
  flex-grow: 1;
}
.play{
  flex-basis: 1;
}
</style>
