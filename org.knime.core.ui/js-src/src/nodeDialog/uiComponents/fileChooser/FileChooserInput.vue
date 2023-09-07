<script setup lang="ts">
import FileChooser from "./FileChooser.vue";
import { ref, type Ref } from "vue";

const active = ref(false);

const file: Ref<false | string> = ref(false);
const chooseFile = (chosen: string) => {
  file.value = chosen;
  active.value = false;
};
</script>

<template>
  <template v-if="file"> {{ file }} selected </template>
  <button
    @click="
      () => {
        active = true;
      }
    "
  >
    Choose File...
  </button>
  <div v-if="active" class="modal-overlay">
    <FileChooser @choose-file="chooseFile" />
  </div>
</template>

<style scoped lang="postcss">
.modal-overlay {
  position: fixed;
  inset: 0;
  padding: 20px;
  z-index: 1000;
  background-color: var(--knime-white);
}
</style>
