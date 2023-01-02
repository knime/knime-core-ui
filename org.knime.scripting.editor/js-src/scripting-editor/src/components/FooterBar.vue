<script>
//

import CloseIcon from 'webapps-common/ui/assets/img/icons/close.svg';
import ShieldCloseIcon from 'webapps-common/ui/assets/img/icons/shield-close.svg';
import Button from 'webapps-common/ui/components/Button.vue';

import { defineComponent } from 'vue';

export default defineComponent({ name: 'FooterBar',
    components: {
        Button,
        CloseIcon,
        ShieldCloseIcon
    } });
</script>

<template>
  <div class="container">
    <Button
      class="abort"
    >
      <ShieldCloseIcon />
    </Button>
    <Button
      class="close"
    >
      <CloseIcon />
    </Button>
  </div>
</template>

<style lang="postcss" scoped>
.container{
  width: 100%;
  display: flex;
  justify-content: space-between;
}
.abort{
    flex: 0 1 10px;
}
.close{
    flex: 0 0 10px;
}
</style>
