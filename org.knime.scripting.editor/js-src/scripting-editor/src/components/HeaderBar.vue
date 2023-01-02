<script>
//

import SettingsIcon from 'webapps-common/ui/assets/img/icons/settings.svg';
import PlayIcon from 'webapps-common/ui/assets/img/icons/play.svg';
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

export default defineComponent({ name: 'HeaderBar',
    components: {
        Button,
        SettingsIcon,
        PlayIcon
    },
    props: {
        isExpanded: {
            type: Boolean,
            default: true
        }
    },
    emits: ['switch'] });
</script>

<template>
  <div class="container">
    <Button
      class="settings"
      @click="$emit('switch')"
    >
      <SettingsIcon />
    </Button>
    <Button>
      <PlayIcon class="play" />
    </Button>
  </div>
</template>


<style lang="postcss" scoped>
.container{
  width: 100%;
  flex-grow: 1;
}
.settings{
  flex-basis: 1;
}
.play{
  flex-basis: 1;
}
</style>
