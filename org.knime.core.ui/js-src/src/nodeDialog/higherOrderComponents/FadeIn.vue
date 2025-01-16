<template>
  <slot />
</template>

<style lang="postcss" scoped>
@import url("../utils/animation.css");

:slotted(*) {
  @mixin fadeIn;
}
</style>
