<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import TabBar from "webapps-common/ui/components/TabBar.vue";

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false,
  },
  modelValue: {
    type: String,
    default: "",
  },
  name: {
    type: String,
    default: "value",
  },
  possibleValues: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["update:modelValue"]);
const data = useVModel(props, "modelValue", emit);
</script>

<template>
  <TabBar
    v-model="data"
    class="scripting-editor-tab-bar"
    :possible-values="props.possibleValues"
    :name="name"
  />
</template>

<style scoped lang="postcss">
.shadow-wrapper {
  margin: 0;

  &::before,
  &::after {
    content: none;
  }

  & :deep(.carousel) {
    padding: 0;
    overflow-y: hidden;

    &::before,
    &::after {
      left: 0;
      right: 0;
      bottom: 9px;
    }
  }
}

.scripting-editor-tab-bar {
  margin-right: 10px;

  & :deep(.tab-bar) {
    padding-top: 8px;
    padding-bottom: 0;

    & span {
      font-size: 13px;
      font-family: Roboto, sans-serif;
      line-height: 61px;
      height: 48px;
      font-weight: 400;
    }
  }
}
</style>
