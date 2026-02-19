<script lang="ts" setup>
defineProps<{
  title: string | null;
}>();
</script>

<template>
  <h4 v-if="title" class="view-title">
    {{ title }}
  </h4>
</template>

<style scoped>
.view-title {
  margin: 0;
  padding: 10px 0 5px;
  color: rgb(70 70 70);
  font-size: 16px;
  line-height: 20px;
}
</style>
